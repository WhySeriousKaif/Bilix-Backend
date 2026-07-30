package com.molla.service.impl;

import com.molla.domain.PaymentType;
import com.molla.exceptions.UserException;
import com.molla.mapper.ShiftReportMapper;
import com.molla.model.*;
import com.molla.payload.dto.ShiftReportDto;
import com.molla.repository.*;
import com.molla.service.ShiftReportService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftReportServiceImpl implements ShiftReportService {

    private final ShiftReportRepository shiftReportRepository;
    private final UserService userService;
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final BranchRepository branchRepository;
    private final AccessControlService accessControlService;

    @Override
    public ShiftReportDto startShift(Long cashierId, Long branchId, LocalDateTime shiftStart) throws Exception {
        // A shift is always started for whoever is actually authenticated —
        // never trust a cashierId supplied by the client (that would let anyone
        // start/act on a shift as a different cashier).
        User cashier = userService.getCurrentUser();

        // Use current time if shiftStart is null
        if (shiftStart == null) {
            shiftStart = LocalDateTime.now();
        }

        // Check if shift already started today
        LocalDateTime startOfDay = shiftStart.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = shiftStart.withHour(23).withMinute(59).withSecond(59);

        Optional<ShiftReport> existing = shiftReportRepository.findByCashierAndShiftStartBetween(
                cashier, startOfDay, endOfDay);

        if (existing.isPresent() && existing.get().getShiftEnd() == null) {
            // Return existing active shift
            return ShiftReportMapper.toDto(existing.get());
        }

        // Get branch - an explicit branchId must still be one this cashier can access;
        // otherwise fall back to their assigned branch.
        Branch branch;
        if (branchId != null) {
            accessControlService.requireBranchAccess(cashier, branchId);
            branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new Exception("Branch not found"));
        } else {
            branch = cashier.getBranch();
            if (branch == null) {
                throw new Exception("Your account is not assigned to a branch. Contact your manager.");
            }
        }

        // Create shift report
        ShiftReport shiftReport = ShiftReport.builder()
                .cashier(cashier)
                .branch(branch)
                .shiftStart(shiftStart)
                .build();

        ShiftReport savedReport = shiftReportRepository.save(shiftReport);
        return ShiftReportMapper.toDto(savedReport);
    }

    @Override
    public ShiftReportDto endShift(Long shiftReportId, LocalDateTime shiftEnd) throws Exception {
        User currentUser = userService.getCurrentUser();

        // Find the shift report - if shiftReportId is null, find active shift
        ShiftReport shiftReport;
        if (shiftReportId != null) {
            shiftReport = shiftReportRepository.findById(shiftReportId)
                    .orElseThrow(() -> new Exception("Shift report not found"));
            // A shift report can only be ended by the cashier it belongs to (or an admin) —
            // otherwise this would overwrite someone else's shift with the caller's own totals.
            accessControlService.requireSelfOrAdmin(currentUser,
                    shiftReport.getCashier() != null ? shiftReport.getCashier().getId() : null);
        } else {
            shiftReport = shiftReportRepository
                    .findTopByCashierAndShiftEndIsNullOrderByShiftStartDesc(currentUser)
                    .orElseThrow(() -> new Exception("No active shift found for cashier"));
        }

        // Use current time if shiftEnd is null
        if (shiftEnd == null) {
            shiftEnd = LocalDateTime.now();
        }

        shiftReport.setShiftEnd(shiftEnd);

        // Get orders for this shift
        List<Order> orders = orderRepository.findByCashierAndCreatedAtBetween(
                currentUser, shiftReport.getShiftStart(), shiftReport.getShiftEnd());

        // Calculate totals
        double totalSales = orders.stream()
                .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0.0)
                .sum();

        int totalOrders = orders.size();

        // Get refunds for this shift
        List<Refund> refunds = refundRepository.findByCashierAndCreatedAtBetween(
                currentUser, shiftReport.getShiftStart(), shiftReport.getShiftEnd());

        double totalRefunds = refunds.stream()
                .mapToDouble(refund -> refund.getAmount() != null ? refund.getAmount() : 0.0)
                .sum();

        double netSales = totalSales - totalRefunds;

        // Set calculated values
        shiftReport.setTotalSales(totalSales);
        shiftReport.setTotalRefunds(totalRefunds);
        shiftReport.setNetSales(netSales);
        shiftReport.setTotalOrders(totalOrders);

        // Set transient fields
        shiftReport.setRecentOrders(getRecentOrders(orders));
        shiftReport.setTopSellingProducts(getTopSellingProducts(orders));
        shiftReport.setPaymentSummaries(getPaymentSummaries(orders, totalSales));
        shiftReport.setRefunds(refunds);

        // Save and return
        ShiftReport savedReport = shiftReportRepository.save(shiftReport);
        return ShiftReportMapper.toDto(savedReport);
    }

    @Override
    public ShiftReportDto getShiftReportById(Long id) throws Exception {
        ShiftReport report = shiftReportRepository.findById(id)
                .orElseThrow(() -> new Exception("Shift report not found with given id " + id));
        accessControlService.requireBranchAccess(userService.getCurrentUser(), report.getBranch().getId());
        return ShiftReportMapper.toDto(report);
    }

    @Override
    public List<ShiftReportDto> getAllShiftReports() throws Exception {
        accessControlService.requireAdmin(userService.getCurrentUser());
        List<ShiftReport> reports = shiftReportRepository.findAll();
        return reports.stream()
                .map(ShiftReportMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShiftReportDto> getShiftReportsByBranchId(Long branchId) throws Exception {
        accessControlService.requireBranchAccess(userService.getCurrentUser(), branchId);
        List<ShiftReport> reports = shiftReportRepository.findByBranchId(branchId);
        return reports.stream()
                .map(ShiftReportMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShiftReportDto> getShiftReportsByCashierId(Long cashierId) throws Exception {
        User currentUser = userService.getCurrentUser();
        User targetCashier = userService.getUserById(cashierId);
        if (targetCashier.getBranch() != null) {
            accessControlService.requireBranchAccess(currentUser, targetCashier.getBranch().getId());
        } else {
            accessControlService.requireSelfOrAdmin(currentUser, cashierId);
        }
        List<ShiftReport> reports = shiftReportRepository.findByCashierId(cashierId);
        return reports.stream()
                .map(ShiftReportMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ShiftReportDto getCurrentShiftProgress(Long cashierId) throws UserException {
        User currentUser = userService.getCurrentUser();
        User user;
        if (cashierId != null) {
            user = userService.getUserById(cashierId);
            if (user.getBranch() != null) {
                accessControlService.requireBranchAccess(currentUser, user.getBranch().getId());
            } else {
                accessControlService.requireSelfOrAdmin(currentUser, cashierId);
            }
        } else {
            user = currentUser;
        }

        ShiftReport shift = shiftReportRepository
                .findTopByCashierAndShiftEndIsNullOrderByShiftStartDesc(user)
                .orElse(null);

        // If no active shift, return an empty progress snapshot
        if (shift == null) {
            ShiftReportDto emptyReport = ShiftReportDto.builder()
                    .cashier(com.molla.mapper.UserMapper.toDto(user))
                    .totalOrders(0)
                    .totalSales(0.0)
                    .totalRefunds(0.0)
                    .netSale(0.0)
                    .paymentSummaries(Collections.emptyList())
                    .topSellingProducts(Collections.emptyList())
                    .recentOrders(Collections.emptyList())
                    .refunds(Collections.emptyList())
                    .build();
            return emptyReport;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Order> orders = orderRepository.findByCashierAndCreatedAtBetween(
                user, shift.getShiftStart(), now);

        // Calculate current progress
        double totalSales = orders.stream()
                .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0.0)
                .sum();

        int totalOrders = orders.size();

        List<Refund> refunds = refundRepository.findByCashierAndCreatedAtBetween(
                user, shift.getShiftStart(), now);

        double totalRefunds = refunds.stream()
                .mapToDouble(refund -> refund.getAmount() != null ? refund.getAmount() : 0.0)
                .sum();

        double netSales = totalSales - totalRefunds;

        // Set calculated values
        shift.setTotalSales(totalSales);
        shift.setTotalRefunds(totalRefunds);
        shift.setNetSales(netSales);
        shift.setTotalOrders(totalOrders);
        shift.setRecentOrders(getRecentOrders(orders));
        shift.setTopSellingProducts(getTopSellingProducts(orders));
        shift.setPaymentSummaries(getPaymentSummaries(orders, totalSales));
        shift.setRefunds(refunds);

        return ShiftReportMapper.toDto(shift);
    }

    @Override
    public ShiftReportDto getShiftByCashierAndDate(Long cashierId, LocalDateTime date) throws UserException {
        User currentUser = userService.getCurrentUser();
        User cashier = userService.getUserById(cashierId);
        if (cashier == null) {
            throw new UserException("Cashier not found with given id " + cashierId);
        }
        if (cashier.getBranch() != null) {
            accessControlService.requireBranchAccess(currentUser, cashier.getBranch().getId());
        } else {
            accessControlService.requireSelfOrAdmin(currentUser, cashierId);
        }

        LocalDateTime start = date.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = date.withHour(23).withMinute(59).withSecond(59);

        ShiftReport report = shiftReportRepository.findByCashierAndShiftStartBetween(cashier, start, end)
                .orElseThrow(() -> new UserException("Shift report not found for cashier on given date"));

        return ShiftReportMapper.toDto(report);
    }

    // Helper methods
    private List<Product> getTopSellingProducts(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return null;
        }

        Map<Product, Integer> productSalesMap = new HashMap<>();

        for (Order order : orders) {
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    Product product = item.getProduct();
                    if (product != null) {
                        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                        productSalesMap.put(product, productSalesMap.getOrDefault(product, 0) + quantity);
                    }
                }
            }
        }

        // Set quantity sold on products and return sorted list
        return productSalesMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(entry -> {
                    Product product = entry.getKey();
                    // Temporarily store quantity sold in product's quantity field for top selling
                    // (Note: This overwrites stock quantity, but it's fine for display purposes)
                    product.setQuantity(entry.getValue());
                    return product;
                })
                .collect(Collectors.toList());
    }

    private List<PaymentSummary> getPaymentSummaries(List<Order> orders, double totalSales) {
        if (orders == null || orders.isEmpty()) {
            return new ArrayList<>();
        }

        Map<PaymentType, List<Order>> grouped = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getPaymentType() != null ? order.getPaymentType() : PaymentType.CASH));

        List<PaymentSummary> summaries = new ArrayList<>();

        for (Map.Entry<PaymentType, List<Order>> entry : grouped.entrySet()) {
            double amount = entry.getValue().stream()
                    .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0.0)
                    .sum();

            int transactions = entry.getValue().size();
            double percent = totalSales > 0 ? (amount / totalSales) * 100 : 0.0;

            PaymentSummary ps = PaymentSummary.builder()
                    .type(entry.getKey())
                    .totalAmount(amount)
                    .transactionCount(transactions)
                    .percentage(percent)
                    .build();

            summaries.add(ps);
        }

        return summaries;
    }

    private List<Order> getRecentOrders(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return null;
        }
        return orders.stream()
                .sorted((a, b) -> {
                    LocalDateTime aTime = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime bTime = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                    return bTime.compareTo(aTime);
                })
                .limit(10)
                .collect(Collectors.toList());
    }
}
