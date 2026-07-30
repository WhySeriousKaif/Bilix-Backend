package com.molla.service.impl;

import com.molla.mapper.RefundMapper;
import com.molla.model.Branch;
import com.molla.model.Order;
import com.molla.model.Refund;
import com.molla.model.ShiftReport;
import com.molla.model.User;
import com.molla.payload.dto.RefundDto;
import com.molla.repository.OrderRepository;
import com.molla.repository.RefundRepository;
import com.molla.repository.ShiftReportRepository;
import com.molla.service.RefundService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {
    
    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final ShiftReportRepository shiftReportRepository;
    private final AccessControlService accessControlService;

    @Override
    public RefundDto createRefund(RefundDto refundDto) throws Exception {
        // The refund is always attributed to whoever is actually authenticated —
        // never trust a cashier/branch id supplied by the client for this.
        User cashier = userService.getCurrentUser();

        // Get order
        Order order = orderRepository.findById(refundDto.getOrderId())
            .orElseThrow(() -> new Exception("Order not found"));

        Branch branch = order.getBranch();
        if (branch == null) {
            throw new Exception("Order does not have an associated branch");
        }
        accessControlService.requireBranchAccess(cashier, branch.getId());

        // Get shift report if provided
        ShiftReport shiftReport = null;
        if (refundDto.getShiftReportId() != null) {
            try {
                shiftReport = shiftReportRepository.findById(refundDto.getShiftReportId())
                    .orElse(null);
            } catch (Exception e) {
                // Shift report not required, continue without it
            }
        }

        // Create refund
        Refund createdRefund = Refund.builder()
            .order(order)
            .cashier(cashier)
            .branch(branch)
            .reason(refundDto.getReason())
            .amount(refundDto.getAmount())
            .paymentType(refundDto.getPaymentType())
            .shiftReport(shiftReport)
            .createdAt(LocalDateTime.now())
            .build();

        // Save refund
        Refund savedRefund = refundRepository.save(createdRefund);
        
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
    public RefundDto getRefundById(Long refundId) throws Exception {
        Refund refund = refundRepository.findById(refundId)
            .orElseThrow(() -> new Exception("Refund not found"));
        accessControlService.requireBranchAccess(userService.getCurrentUser(), refund.getBranch().getId());
        return RefundMapper.toDto(refund);
    }

    @Override
    public void deleteRefund(Long refundId) throws Exception {
        // Check if refund exists
        this.getRefundById(refundId);
        refundRepository.deleteById(refundId);
    }
}

