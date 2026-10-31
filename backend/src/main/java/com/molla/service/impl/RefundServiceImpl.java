package com.molla.service.impl;

import com.molla.mapper.RefundMapper;
import com.molla.domain.OrderStatus;
import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.NotFoundException;
import com.molla.model.Branch;
import com.molla.model.Order;
import com.molla.model.OrderItem;
import com.molla.model.Product;
import com.molla.model.Refund;
import com.molla.model.RefundItem;
import com.molla.model.ShiftReport;
import com.molla.model.User;
import com.molla.payload.dto.RefundDto;
import com.molla.payload.dto.RefundItemDto;
import com.molla.repository.OrderRepository;
import com.molla.repository.ProductRepository;
import com.molla.repository.RefundRepository;
import com.molla.repository.ShiftReportRepository;
import com.molla.service.RefundService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {
    
    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final ShiftReportRepository shiftReportRepository;
    private final ProductRepository productRepository;
    private final AccessControlService accessControlService;

    @Override
    @Transactional
    @CacheEvict(cacheNames = { "productsByStore", "productsAll", "orders", "recentOrdersByBranch" }, allEntries = true)
    public RefundDto createRefund(RefundDto refundDto) throws Exception {
        // The refund is always attributed to whoever is actually authenticated —
        // never trust a cashier/branch id supplied by the client for this.
        User cashier = userService.getCurrentUser();

        // Get order
        if (refundDto.getOrderId() == null) {
            throw new BadRequestException("Order ID is required");
        }
        if (refundDto.getReason() == null || refundDto.getReason().isBlank()) {
            throw new BadRequestException("Refund reason is required");
        }
        if (refundDto.getPaymentType() == null) {
            throw new BadRequestException("Refund payment method is required");
        }
        if (refundDto.getItems() == null || refundDto.getItems().isEmpty()) {
            throw new BadRequestException("Select at least one item to refund");
        }

        Order order = orderRepository.findByIdForUpdate(refundDto.getOrderId())
            .orElseThrow(() -> new NotFoundException("Order not found"));

        Branch branch = order.getBranch();
        if (branch == null) {
            throw new Exception("Order does not have an associated branch");
        }
        accessControlService.requireBranchAccess(cashier, branch.getId());
        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.PARTIALLY_REFUNDED) {
            throw new BadRequestException("This order cannot be refunded");
        }

        // Get shift report if provided
        ShiftReport shiftReport = null;
        if (refundDto.getShiftReportId() != null) {
            shiftReport = shiftReportRepository.findById(refundDto.getShiftReportId())
                .orElseThrow(() -> new NotFoundException("Shift report not found"));
            if (shiftReport.getCashier() == null || !shiftReport.getCashier().getId().equals(cashier.getId())
                    || shiftReport.getBranch() == null || !shiftReport.getBranch().getId().equals(branch.getId())
                    || shiftReport.getShiftEnd() != null) {
                throw new BadRequestException("Refund must use the cashier's active shift");
            }
        }

        Map<Long, OrderItem> orderItems = order.getOrderItems().stream()
            .collect(Collectors.toMap(OrderItem::getId, item -> item));
        Map<Long, Integer> alreadyRefunded = new HashMap<>();
        List<Refund> existingRefunds = refundRepository.findByOrderId(order.getId());
        boolean hasLegacyRefund = existingRefunds.stream()
            .anyMatch(existing -> existing.getAmount() != null && existing.getAmount() > 0
                && (existing.getItems() == null || existing.getItems().isEmpty()));
        if (hasLegacyRefund) {
            throw new BadRequestException("This order has a legacy refund without item details; manual review is required");
        }
        existingRefunds.stream()
            .filter(existing -> existing.getItems() != null)
            .flatMap(existing -> existing.getItems().stream())
            .forEach(item -> alreadyRefunded.merge(item.getOrderItem().getId(), item.getQuantity(), Integer::sum));

        List<RefundItem> refundItems = new ArrayList<>();
        Map<Long, Integer> quantitiesInRequest = new HashMap<>();
        double calculatedAmount = 0.0;
        for (RefundItemDto itemDto : refundDto.getItems()) {
            if (itemDto.getOrderItemId() == null || itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                throw new BadRequestException("Each refund item requires a valid order item and quantity");
            }
            if (quantitiesInRequest.put(itemDto.getOrderItemId(), itemDto.getQuantity()) != null) {
                throw new BadRequestException("The same order item cannot appear twice in one refund");
            }
            OrderItem orderItem = orderItems.get(itemDto.getOrderItemId());
            if (orderItem == null) {
                throw new BadRequestException("Refund item does not belong to this order");
            }
            int remaining = orderItem.getQuantity() - alreadyRefunded.getOrDefault(orderItem.getId(), 0);
            if (itemDto.getQuantity() > remaining) {
                throw new BadRequestException("Only " + remaining + " unit(s) of "
                    + orderItem.getProduct().getName() + " remain refundable");
            }
            double unitPrice = orderItem.getPrice() != null ? orderItem.getPrice() : 0.0;
            double itemAmount = unitPrice * itemDto.getQuantity();
            calculatedAmount += itemAmount;
            refundItems.add(RefundItem.builder()
                .orderItem(orderItem)
                .product(orderItem.getProduct())
                .quantity(itemDto.getQuantity())
                .unitPrice(unitPrice)
                .amount(itemAmount)
                .build());
        }
        if (calculatedAmount <= 0) {
            throw new BadRequestException("Refund amount must be greater than zero");
        }

        // Create refund
        Refund createdRefund = Refund.builder()
            .order(order)
            .cashier(cashier)
            .branch(branch)
            .reason(refundDto.getReason())
            .amount(calculatedAmount)
            .paymentType(refundDto.getPaymentType())
            .shiftReport(shiftReport)
            .createdAt(LocalDateTime.now())
            .items(refundItems)
            .build();

        refundItems.forEach(item -> item.setRefund(createdRefund));

        // Persist the audit record, restore returned stock, and update the order as one transaction.
        Refund savedRefund = refundRepository.save(createdRefund);
        refundItems.forEach(item -> {
            Product product = item.getProduct();
            product.setQuantity((product.getQuantity() == null ? 0 : product.getQuantity()) + item.getQuantity());
            productRepository.save(product);
            alreadyRefunded.merge(item.getOrderItem().getId(), item.getQuantity(), Integer::sum);
        });

        boolean fullyRefunded = order.getOrderItems().stream()
            .allMatch(item -> alreadyRefunded.getOrDefault(item.getId(), 0) >= item.getQuantity());
        order.setStatus(fullyRefunded ? OrderStatus.REFUNDED : OrderStatus.PARTIALLY_REFUNDED);
        orderRepository.save(order);
        
        return RefundMapper.toDto(savedRefund);
    }

    @Override
    public List<RefundDto> getAllRefunds() throws Exception {
        accessControlService.requireAdmin(userService.getCurrentUser());
        return refundRepository.findAll().stream()
            .map(RefundMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundByCashier(Long cashierId) throws Exception {
        User currentUser = userService.getCurrentUser();
        User targetCashier = userService.getUserById(cashierId);
        if (targetCashier.getBranch() != null) {
            accessControlService.requireBranchAccess(currentUser, targetCashier.getBranch().getId());
        } else {
            accessControlService.requireSelfOrAdmin(currentUser, cashierId);
        }
        return refundRepository.findByCashierId(cashierId).stream()
            .map(RefundMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundByShiftReport(Long shiftReportId) throws Exception {
        ShiftReport shiftReport = shiftReportRepository.findById(shiftReportId)
            .orElseThrow(() -> new Exception("Shift report not found"));
        accessControlService.requireBranchAccess(userService.getCurrentUser(), shiftReport.getBranch().getId());
        return refundRepository.findByShiftReportId(shiftReportId).stream()
            .map(RefundMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundByCashierAndDateRange(Long cashierId, LocalDateTime startDate, LocalDateTime endDate) throws Exception {
        User currentUser = userService.getCurrentUser();
        User cashier = userService.getUserById(cashierId);
        if (cashier.getBranch() != null) {
            accessControlService.requireBranchAccess(currentUser, cashier.getBranch().getId());
        } else {
            accessControlService.requireSelfOrAdmin(currentUser, cashierId);
        }
        return refundRepository.findByCashierAndCreatedAtBetween(cashier, startDate, endDate).stream()
            .map(RefundMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundByBranch(Long branchId) throws Exception {
        accessControlService.requireBranchAccess(userService.getCurrentUser(), branchId);
        return refundRepository.findByBranchId(branchId).stream()
            .map(RefundMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<RefundDto> getRefundByOrder(Long orderId) throws Exception {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));
        accessControlService.requireBranchAccess(userService.getCurrentUser(), order.getBranch().getId());
        return refundRepository.findByOrderId(orderId).stream()
            .map(RefundMapper::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public RefundDto getRefundById(Long refundId) throws Exception {
        Refund refund = refundRepository.findById(refundId)
            .orElseThrow(() -> new Exception("Refund not found"));
        accessControlService.requireBranchAccess(userService.getCurrentUser(), refund.getBranch().getId());
        return RefundMapper.toDto(refund);
    }

    @Override
    public void deleteRefund(Long refundId) throws Exception {
        throw new BadRequestException("Refund records are immutable and cannot be deleted");
    }
}
