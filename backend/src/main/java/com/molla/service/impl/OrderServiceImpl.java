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
import com.molla.payload.dto.SystemSalesSummaryDto;
import com.molla.repository.BranchRepository;
import com.molla.repository.CustomerRepository;
import com.molla.repository.OrderRepository;
import com.molla.repository.ProductRepository;
import com.molla.repository.RefundRepository;
import com.molla.repository.UserRepository;
import com.molla.repository.InventoryRepository;
import com.molla.service.OrderService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

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
    private final RefundRepository refundRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {
            "productsByStore", "productsAll", "orders", "recentOrdersByBranch",
            "inventory", "inventoriesByBranch"
    }, allEntries = true)
    public OrderDto createOrder(OrderDto orderDto,String idempotencyKey) throws Exception {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");

        }
        Optional<Order>existingOrder=orderRepository.findByIdempotencyKey(idempotencyKey);

        if(existingOrder.isPresent()){
            return OrderMapper.toDto(existingOrder.get());
        }

        // The order is always attributed to whoever is actually authenticated —
        // never trust a branchId/cashierId supplied by the client for this.
        User cashier = userService.getCurrentUser();
        Branch branch = cashier.getBranch();

        if (branch == null) {
            throw new BadRequestException("Your account is not assigned to a branch. Contact your manager.");
        }

        // Get customer if customerId is provided
        Customer customer = null;
        if (orderDto.getCustomerId() != null) {
            customer = customerRepository.findById(orderDto.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("Customer not found"));
        }

        // Create order
        Order order = Order.builder()
                .branch(branch)
                .cashier(cashier)
                .customer(customer)
                .paymentType(orderDto.getPaymentType())
                .status(OrderStatus.COMPLETED)
                .idempotencyKey(idempotencyKey)
                .build();

        // Create order items
        if (orderDto.getOrderItems() == null || orderDto.getOrderItems().isEmpty()) {
            throw new BadRequestException("Order must have at least one item");
        }

        List<OrderItem> orderItems = orderDto.getOrderItems().stream()
                .sorted(java.util.Comparator.comparing(item -> item.getProductId() != null
                        ? item.getProductId()
                        : item.getProduct() != null ? item.getProduct().getId() : Long.MAX_VALUE))
                .map((OrderItemDto itemDto) -> {
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
            if (product.getStore() == null || branch.getStore() == null
                    || !product.getStore().getId().equals(branch.getStore().getId())) {
                throw new BadRequestException("Product does not belong to this branch's store");
            }

            Double itemPrice;
            if (product.getPrice() != null) {
                itemPrice = product.getPrice();
            } else if (product.getSellingPrice() != null) {
                itemPrice = product.getSellingPrice();
            } else if (product.getMrp() != null) {
                itemPrice = product.getMrp();
            } else {
                throw new BadRequestException("Product price is missing for product: " + product.getName());
            }
            Integer quantity = itemDto.getQuantity() != null ? itemDto.getQuantity() : 1;
            if (quantity <= 0) {
                throw new BadRequestException("Quantity must be positive for " + product.getName());
            }

            var inventory = inventoryRepository.findByProductIdAndBranchIdForUpdate(productId, branch.getId())
                    .orElseThrow(() -> new BadRequestException("Product is not stocked at this branch: " + product.getName()));
            if (inventory.getQuantity() < quantity) {
                throw new BadRequestException("Not enough stock for " + product.getName() + " (available: "
                        + inventory.getQuantity() + ", requested: " + quantity + ")");
            }
            inventory.setQuantity(inventory.getQuantity() - quantity);
            inventoryRepository.save(inventory);

            Double totalPrice = itemPrice * quantity;

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
        return getOrdersByBranchPaged(branchId, page, size, sortBy, direction, null, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> getOrdersByBranchPaged(Long branchId, int page, int size, String sortBy, String direction,
            String search, String status, String payment, String period) throws Exception {
        accessControlService.requireBranchAccess(userService.getCurrentUser(), branchId);
        if (page < 0)
            page = 0;
        if (size <= 0 || size > 100)
            size = 10;
        if (sortBy == null || !Set.of("id", "createdAt", "totalAmount").contains(sortBy)) {
            sortBy = "createdAt";
        }
        if (direction == null || direction.isBlank()) {
            direction = "desc";
        }

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        PageRequest pageable = PageRequest.of(page, size, sort);

        LocalDateTime from = switch (period == null ? "all" : period.toLowerCase()) {
            case "today" -> LocalDate.now().atStartOfDay();
            case "week" -> LocalDate.now().minusDays(6).atStartOfDay();
            case "month" -> LocalDate.now().withDayOfMonth(1).atStartOfDay();
            default -> null;
        };
        OrderStatus orderStatus = status == null || status.isBlank() || "all".equalsIgnoreCase(status)
                ? null : OrderStatus.valueOf(status.toUpperCase());
        PaymentType paymentType = payment == null || payment.isBlank() || "all".equalsIgnoreCase(payment)
                ? null : PaymentType.valueOf(payment.toUpperCase());
        String term = search == null || search.isBlank() ? null : search.trim();
        Long orderId = null;
        if (term != null) {
            try { orderId = Long.parseLong(term.replaceFirst("^#", "")); }
            catch (NumberFormatException ignored) { /* Name search. */ }
        }
        Long parsedOrderId = orderId;
        Specification<Order> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("branch").get("id"), branchId));
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (orderStatus != null) predicates.add(builder.equal(root.get("status"), orderStatus));
            if (paymentType != null) predicates.add(builder.equal(root.get("paymentType"), paymentType));
            if (term != null) {
                var customer = root.join("customer", JoinType.LEFT);
                Predicate customerName = builder.like(
                        builder.lower(customer.get("name")),
                        "%" + term.toLowerCase(Locale.ROOT) + "%");
                predicates.add(parsedOrderId == null
                        ? customerName
                        : builder.or(builder.equal(root.get("id"), parsedOrderId), customerName));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Page<Order> pageResult = orderRepository.findAll(specification, pageable);
        return pageResult.map(OrderMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto> getSystemSalesPaged(int page, int size, String sortBy, String direction,
            String search, OrderStatus status, Long storeId) {
        accessControlService.requireAdmin(userService.getCurrentUser());
        page = Math.max(page, 0);
        size = size <= 0 || size > 100 ? 10 : size;
        Set<String> allowedSorts = Set.of("id", "createdAt", "totalAmount", "status", "paymentType");
        sortBy = allowedSorts.contains(sortBy) ? sortBy : "createdAt";
        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        Specification<Order> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            var branch = root.join("branch", JoinType.LEFT);
            var store = branch.join("store", JoinType.LEFT);
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (storeId != null) predicates.add(builder.equal(store.get("id"), storeId));
            if (normalizedSearch != null) {
                String pattern = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
                var customer = root.join("customer", JoinType.LEFT);
                var cashier = root.join("cashier", JoinType.LEFT);
                predicates.add(builder.or(
                        builder.like(builder.lower(customer.get("name")), pattern),
                        builder.like(builder.lower(cashier.get("fullName")), pattern),
                        builder.like(builder.lower(cashier.get("email")), pattern),
                        builder.like(builder.lower(store.get("brand")), pattern)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return orderRepository.findAll(specification, PageRequest.of(page, size, sort))
                .map(OrderMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemSalesSummaryDto getSystemSalesSummary() {
        return getSystemSalesSummary(7);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemSalesSummaryDto getSystemSalesSummary(int days) {
        accessControlService.requireAdmin(userService.getCurrentUser());
        if (days != 7 && days != 30) {
            throw new BadRequestException("Sales period must be 7 or 30 days");
        }
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime periodStart = today.minusDays(days - 1L).atStartOfDay();

        double totalSales = orderRepository.sumSystemSales();
        double refundAmount = refundRepository.sumSystemRefundAmount();

        Map<LocalDate, Double> dailyTotals = new LinkedHashMap<>();
        for (int day = days - 1; day >= 0; day--) {
            dailyTotals.put(today.minusDays(day), 0.0);
        }
        orderRepository.findSalesRowsBetween(periodStart, tomorrowStart).forEach(row -> {
            LocalDate date;
            if (row[0] instanceof LocalDateTime ldt) {
                date = ldt.toLocalDate();
            } else if (row[0] instanceof java.sql.Timestamp ts) {
                date = ts.toLocalDateTime().toLocalDate();
            } else if (row[0] instanceof java.sql.Date d) {
                date = d.toLocalDate();
            } else if (row[0] instanceof java.time.Instant instant) {
                date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            } else {
                date = LocalDate.parse(row[0].toString().substring(0, 10));
            }
            double amount = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
            dailyTotals.computeIfPresent(date, (key, value) -> value + amount);
        });

        List<SystemSalesSummaryDto.NamedAmountDto> dailySales = dailyTotals.entrySet().stream()
                .map(entry -> new SystemSalesSummaryDto.NamedAmountDto(entry.getKey().toString(), entry.getValue()))
                .toList();
        List<SystemSalesSummaryDto.NamedAmountDto> paymentMethods = orderRepository.sumSalesByPaymentType().stream()
                .map(row -> new SystemSalesSummaryDto.NamedAmountDto(
                        row[0] == null ? "CASH" : row[0].toString(),
                        ((Number) row[1]).doubleValue()))
                .toList();

        return SystemSalesSummaryDto.builder()
                .totalSales(totalSales)
                .ordersToday(orderRepository.countByCreatedAtBetween(todayStart, tomorrowStart))
                .totalRefunds(refundRepository.count())
                .refundAmount(refundAmount)
                .netSales(totalSales - refundAmount)
                .dailySales(dailySales)
                .paymentMethods(paymentMethods)
                .storeSales(toIdSales(orderRepository.sumSalesByStore()))
                .branchSales(toIdSales(orderRepository.sumSalesByBranch()))
                .build();
    }

    private List<SystemSalesSummaryDto.IdSalesDto> toIdSales(List<Object[]> rows) {
        List<SystemSalesSummaryDto.IdSalesDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            if (row[0] == null) continue;
            result.add(new SystemSalesSummaryDto.IdSalesDto(
                    ((Number) row[0]).longValue(),
                    ((Number) row[1]).doubleValue(),
                    ((Number) row[2]).longValue()));
        }
        return result;
    }
}
