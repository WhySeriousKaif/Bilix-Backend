package com.molla.service.impl;

import com.molla.domain.OrderStatus;
import com.molla.domain.PaymentType;
import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.NotFoundException;
import com.molla.mapper.OrderMapper;
import com.molla.model.Branch;
import com.molla.model.Customer;
import com.molla.model.Order;
import com.molla.model.OrderItem;
import com.molla.model.Product;
import com.molla.model.User;
import com.molla.payload.dto.OrderDto;
import com.molla.payload.dto.OrderItemDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.CustomerRepository;
import com.molla.repository.OrderRepository;
import com.molla.repository.ProductRepository;
import com.molla.repository.UserRepository;
import com.molla.service.OrderService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final UserService userService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;

    @Override
    @CacheEvict(cacheNames = { "productsByStore", "productsAll" }, allEntries = true)
    public OrderDto createOrder(OrderDto orderDto) throws Exception {
        // The order is always attributed to whoever is actually authenticated —
        // never trust a branchId/cashierId supplied by the client for this.
        User cashier = userService.getCurrentUser();
        Branch branch = cashier.getBranch();

        if (branch == null) {
            throw new Exception("Your account is not assigned to a branch. Contact your manager.");
        }

        // Get customer if customerId is provided
        Customer customer = null;
        if (orderDto.getCustomerId() != null) {
            customer = customerRepository.findById(orderDto.getCustomerId())
                    .orElseThrow(() -> new Exception("Customer not found"));
        }

        // Create order
        Order order = Order.builder()
                .branch(branch)
                .cashier(cashier)
                .customer(customer)
                .paymentType(orderDto.getPaymentType())
                .status(orderDto.getStatus() != null ? orderDto.getStatus() : OrderStatus.PENDING)
                .build();

        // Create order items
        if (orderDto.getOrderItems() == null || orderDto.getOrderItems().isEmpty()) {
            throw new Exception("Order must have at least one item");
        }

        List<OrderItem> orderItems = orderDto.getOrderItems().stream().map((OrderItemDto itemDto) -> {
            // Get product - check if productId is provided, otherwise use product.getId()
            final Long productId;
            if (itemDto.getProductId() != null) {
                productId = itemDto.getProductId();
            } else if (itemDto.getProduct() != null && itemDto.getProduct().getId() != null) {
                productId = itemDto.getProduct().getId();
            } else {
                throw new BadRequestException("Product ID is required for order item");
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

            Double itemPrice = itemDto.getPrice();
            if (itemPrice == null) {
                if (product.getPrice() != null) {
                    itemPrice = product.getPrice();
                } else if (product.getSellingPrice() != null) {
                    itemPrice = product.getSellingPrice();
                } else if (product.getMrp() != null) {
                    itemPrice = product.getMrp();
                } else {
                    throw new BadRequestException("Product price is missing for product: " + product.getName());
                }
            }
            Integer quantity = itemDto.getQuantity() != null ? itemDto.getQuantity() : 1;
            Double totalPrice = itemPrice * quantity;

            // Deduct sold quantity from stock so the catalog (and every dashboard reading it)
            // reflects what's actually left on the shelf.
            if (product.getQuantity() != null) {
                if (product.getQuantity() < quantity) {
                    throw new BadRequestException(
                            "Not enough stock for " + product.getName() + " (available: " + product.getQuantity()
                                    + ", requested: " + quantity + ")");
                }
                product.setQuantity(product.getQuantity() - quantity);
                productRepository.save(product);
            }

            return OrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .price(itemPrice)
                    .totalPrice(totalPrice)
                    .order(order)
                    .build();
        }).collect(Collectors.toList());

        // Calculate total amount
        double totalAmount = orderItems.stream()
                .mapToDouble(item -> item.getTotalPrice() != null ? item.getTotalPrice() : 0.0)
                .sum();

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        // Save order
        Order savedOrder = orderRepository.save(order);
        return OrderMapper.toDto(savedOrder);
    }

    @Override
    @CacheEvict(cacheNames = { "orders", "recentOrdersByBranch" }, allEntries = true)
    public OrderDto updateOrder(Long id, OrderDto orderDto) throws Exception {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new Exception("Order not found"));
        accessControlService.requireBranchAccess(userService.getCurrentUser(), order.getBranch().getId());

        // Update order fields
        if (orderDto.getPaymentType() != null) {
            order.setPaymentType(orderDto.getPaymentType());
        }
        if (orderDto.getStatus() != null) {
            order.setStatus(orderDto.getStatus());
        }

        Order updatedOrder = orderRepository.save(order);
        return OrderMapper.toDto(updatedOrder);
    }

    @Override
    @CacheEvict(cacheNames = { "orders", "recentOrdersByBranch" }, allEntries = true)
    public void deleteOrder(Long id) throws Exception {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new Exception("Order not found with id " + id));
        accessControlService.requireBranchAccess(userService.getCurrentUser(), order.getBranch().getId());
        orderRepository.delete(order);
    }

    @Override
    @Cacheable(cacheNames = "orders", key = "#id")
    public OrderDto getOrderById(Long id) throws Exception {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new Exception("Order not found"));
        accessControlService.requireBranchAccess(userService.getCurrentUser(), order.getBranch().getId());
        return OrderMapper.toDto(order);
    }

    @Override
    public List<OrderDto> getOrdersByBranch(Long branchId, Long customerId, Long cashierId, PaymentType paymentType,
            OrderStatus orderStatus) throws Exception {
        accessControlService.requireBranchAccess(userService.getCurrentUser(), branchId);
        return orderRepository.findByBranchId(branchId).stream()
                .filter(order -> customerId == null
                        || (order.getCustomer() != null && order.getCustomer().getId().equals(customerId)))
                .filter(order -> cashierId == null
                        || (order.getCashier() != null && order.getCashier().getId().equals(cashierId)))
                .filter(order -> paymentType == null || order.getPaymentType() == paymentType)
                .filter(order -> orderStatus == null || order.getStatus() == orderStatus)
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getOrdersByCashier(Long cashierId) throws Exception {
        User currentUser = userService.getCurrentUser();
        User targetCashier = userRepository.findById(cashierId).orElse(null);
        if (targetCashier != null && targetCashier.getBranch() != null) {
            accessControlService.requireBranchAccess(currentUser, targetCashier.getBranch().getId());
        } else {
            accessControlService.requireSelfOrAdmin(currentUser, cashierId);
        }
        return orderRepository.findByCashierId(cashierId).stream()
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getTodayOrderbyBranch(Long branchId) throws Exception {
        accessControlService.requireBranchAccess(userService.getCurrentUser(), branchId);
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        return orderRepository.findByBranchIdAndCreatedAtBetween(branchId, start, end).stream()
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDto> getOrderByCustomerId(Long customerId) throws Exception {
        // Not branch-scoped on purpose: a customer's loyalty/order history is looked up by
        // cashiers across branches (e.g. to apply loyalty points), so this stays store-agnostic.
        return orderRepository.findByCustomerId(customerId).stream()
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = "recentOrdersByBranch", key = "#branchId")
    public List<OrderDto> getTop5RecentOrdersByBranchId(Long branchId) throws Exception {
        accessControlService.requireBranchAccess(userService.getCurrentUser(), branchId);
        return orderRepository.findTop5ByBranchIdOrderByCreatedAtDesc(branchId).stream()
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<OrderDto> getOrdersByBranchPaged(Long branchId, int page, int size, String sortBy, String direction)
            throws Exception {
        accessControlService.requireBranchAccess(userService.getCurrentUser(), branchId);
        if (page < 0)
            page = 0;
        if (size <= 0 || size > 100)
            size = 10;
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        if (direction == null || direction.isBlank()) {
            direction = "desc";
        }

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        PageRequest pageable = PageRequest.of(page, size, sort);

        Page<Order> pageResult = orderRepository.findByBranchId(branchId, pageable);
        return pageResult.map(OrderMapper::toDto);
    }
}
