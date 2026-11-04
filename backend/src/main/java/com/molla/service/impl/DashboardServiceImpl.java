package com.molla.service.impl;

import com.molla.domain.OrderStatus;
import com.molla.domain.PaymentType;
import com.molla.exceptions.BadRequestException;
import com.molla.exceptions.NotFoundException;
import com.molla.model.Branch;
import com.molla.model.Order;
import com.molla.model.OrderItem;
import com.molla.model.Refund;
import com.molla.payload.dto.StoreDashboardDto;
import com.molla.payload.dto.StoreDashboardDto.*;
import com.molla.repository.BranchRepository;
import com.molla.repository.OrderRepository;
import com.molla.repository.RefundRepository;
import com.molla.repository.UserRepository;
import com.molla.service.DashboardService;
import com.molla.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Builds the store-admin and branch-manager dashboards from real order rows.
 *
 * <p>One page load issues two range queries (current window + the equally long window
 * directly before it) instead of one request per branch, then derives every figure the
 * UI shows: headline metrics, period-over-period deltas, chart buckets, branch rankings,
 * recent sales, top products and the order-status split.
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yy");
    private static final int TOP_PRODUCTS_LIMIT = 5;
    private static final int RECENT_SALES_LIMIT = 5;

    private final UserService userService;
    private final AccessControlService accessControlService;
    private final OrderRepository orderRepository;
    private final BranchRepository branchRepository;
    private final RefundRepository refundRepository;
    private final UserRepository userRepository;

    @Override
    public StoreDashboardDto getStoreDashboard(Long storeId, String periodKey, String granularityKey) {
        if (storeId == null) {
            throw new BadRequestException("Store id is required");
        }
        accessControlService.requireStoreAccess(userService.getCurrentUser(), storeId);

        Window window = resolveWindow(periodKey, granularityKey);
        List<Branch> branches = branchRepository.findByStoreId(storeId);
        List<Order> orders = orderRepository.findStoreOrdersInRange(
                storeId, window.rangeStart, window.rangeEnd);
        List<Order> previousOrders = orderRepository.findStoreOrdersInRange(
                storeId, window.previousStart, window.rangeStart);

        return buildDashboard(window, orders, previousOrders, branches, storeId, null);
    }

    @Override
    public StoreDashboardDto getBranchDashboard(Long branchId, String periodKey, String granularityKey) {
        if (branchId == null) {
            throw new BadRequestException("Branch id is required");
        }
        accessControlService.requireBranchAccess(userService.getCurrentUser(), branchId);

        Window window = resolveWindow(periodKey, granularityKey);
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found with id: " + branchId));
        List<Order> orders = orderRepository.findBranchOrdersInRange(
                branchId, window.rangeStart, window.rangeEnd);
        List<Order> previousOrders = orderRepository.findBranchOrdersInRange(
                branchId, window.previousStart, window.rangeStart);

        StoreDashboardDto dto = buildDashboard(window, orders, previousOrders, List.of(branch), null, branchId);
        enrichBranch(dto, branchId, window, orders);
        return dto;
    }

    /** Resolves the requested window and the bucketing actually applied to it. */
    private Window resolveWindow(String periodKey, String granularityKey) {
        LocalDate today = LocalDate.now();
        Period period = Period.resolve(periodKey);
        LocalDate start = period.start(today);
        LocalDate endExclusive = today.plusDays(1);
        long days = Math.max(1, ChronoUnit.DAYS.between(start, endExclusive));

        Granularity defaultGranularity = clamp(period.defaultGranularity, days);
        Granularity granularity = clamp(Granularity.resolve(granularityKey, defaultGranularity), days);

        return new Window(
                period,
                start,
                endExclusive,
                days,
                granularity,
                defaultGranularity,
                buildLabels(start, endExclusive, granularity),
                buildLabels(start, endExclusive, defaultGranularity),
                start.atStartOfDay(),
                endExclusive.atStartOfDay(),
                start.minusDays(days).atStartOfDay());
    }

    private StoreDashboardDto buildDashboard(Window window, List<Order> orders, List<Order> previousOrders,
            List<Branch> branches, Long storeId, Long branchId) {
        Period period = window.period;
        LocalDate start = window.start;
        LocalDate endExclusive = window.endExclusive;
        LocalDate today = window.endExclusive.minusDays(1);
        Granularity granularity = window.granularity;
        Granularity defaultGranularity = window.defaultGranularity;
        List<String> labels = window.labels;
        List<String> sparklineLabels = window.sparklineLabels;

        // ---- chart buckets (what the trend charts plot) ----
        double[] trendTotals = new double[labels.size()];
        Map<String, double[]> trendByBranch = new LinkedHashMap<>();
        for (Branch branch : branches) {
            trendByBranch.put(branch.getName(), new double[labels.size()]);
        }

        // ---- sparkline buckets (metric cards; always the period's default bucketing) ----
        double[] sparkSales = new double[sparklineLabels.size()];
        double[] sparkOrders = new double[sparklineLabels.size()];
        double[] sparkAov = new double[sparklineLabels.size()];
        List<Set<Long>> sparkCustomers = new ArrayList<>(sparklineLabels.size());
        for (int i = 0; i < sparklineLabels.size(); i++) {
            sparkCustomers.add(new HashSet<>());
        }

        Totals current = new Totals();
        Map<Long, BranchTotals> branchTotals = new LinkedHashMap<>();
        for (Branch branch : branches) {
            branchTotals.put(branch.getId(), new BranchTotals(branch.getId(), branch.getName()));
        }
        Map<OrderStatus, Long> statusCounts = new EnumMap<>(OrderStatus.class);
        Map<Long, ProductTotals> productTotals = new LinkedHashMap<>();
        List<Order> recent = new ArrayList<>();

        for (Order order : orders) {
            LocalDateTime createdAt = order.getCreatedAt();
            double amount = order.getTotalAmount() != null ? order.getTotalAmount() : 0.0;
            Long customerId = order.getCustomer() != null ? order.getCustomer().getId() : null;

            current.add(amount, customerId);

            int trendIndex = bucketIndex(createdAt, start, granularity, labels.size());
            trendTotals[trendIndex] += amount;
            if (order.getBranch() != null) {
                double[] series = trendByBranch.computeIfAbsent(
                        order.getBranch().getName(), key -> new double[labels.size()]);
                if (series.length == labels.size()) {
                    series[trendIndex] += amount;
                }
            }

            int sparkIndex = bucketIndex(createdAt, start, defaultGranularity, sparklineLabels.size());
            sparkSales[sparkIndex] += amount;
            sparkOrders[sparkIndex] += 1;
            if (customerId != null) {
                sparkCustomers.get(sparkIndex).add(customerId);
            }

            if (order.getBranch() != null) {
                branchTotals.computeIfAbsent(order.getBranch().getId(),
                        id -> new BranchTotals(id, order.getBranch().getName())).add(amount);
            }

            OrderStatus status = order.getStatus() != null ? order.getStatus() : OrderStatus.PENDING;
            statusCounts.merge(status, 1L, Long::sum);

            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    if (item.getProduct() == null) {
                        continue;
                    }
                    ProductTotals product = productTotals.computeIfAbsent(
                            item.getProduct().getId(),
                            id -> new ProductTotals(id, item.getProduct().getName(), item.getProduct().getImage()));
                    int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                    double revenue = item.getTotalPrice() != null
                            ? item.getTotalPrice()
                            : (item.getPrice() != null ? item.getPrice() * quantity : 0.0);
                    product.add(quantity, revenue);
                }
            }

            if (recent.size() < RECENT_SALES_LIMIT) {
                recent.add(order);
            }
        }

        Totals previous = new Totals();
        for (Order order : previousOrders) {
            previous.add(order.getTotalAmount() != null ? order.getTotalAmount() : 0.0,
                    order.getCustomer() != null ? order.getCustomer().getId() : null);
        }

        for (int i = 0; i < sparklineLabels.size(); i++) {
            sparkAov[i] = sparkOrders[i] == 0 ? 0.0 : sparkSales[i] / sparkOrders[i];
        }

        Map<String, List<Double>> sparklines = new HashMap<>();
        sparklines.put("totalSales", toList(sparkSales));
        sparklines.put("totalOrders", toList(sparkOrders));
        sparklines.put("uniqueCustomers", sparkCustomers.stream().map(set -> (double) set.size()).toList());
        sparklines.put("averageOrderValue", toList(sparkAov));

        List<TrendPointDto> salesTrend = new ArrayList<>(labels.size());
        for (int i = 0; i < labels.size(); i++) {
            final int index = i;
            Map<String, Double> byBranch = new LinkedHashMap<>();
            trendByBranch.forEach((name, series) -> byBranch.put(name, series[index]));
            salesTrend.add(TrendPointDto.builder()
                    .label(labels.get(i))
                    .total(round2(trendTotals[i]))
                    .branches(byBranch)
                    .build());
        }

        double totalSales = current.sales;
        long totalOrders = current.orders;
        double aov = totalOrders == 0 ? 0.0 : totalSales / totalOrders;
        double previousAov = previous.orders == 0 ? 0.0 : previous.sales / previous.orders;

        StoreDashboardDto.MetricsDto metrics = StoreDashboardDto.MetricsDto.builder()
                .totalSales(round2(totalSales))
                .totalSalesPrevious(round2(previous.sales))
                .totalSalesChangePct(changePct(totalSales, previous.sales))
                .totalOrders(totalOrders)
                .totalOrdersPrevious(previous.orders)
                .totalOrdersChangePct(changePct(totalOrders, previous.orders))
                .uniqueCustomers((long) current.customerIds.size())
                .uniqueCustomersPrevious((long) previous.customerIds.size())
                .uniqueCustomersChangePct(changePct(current.customerIds.size(), previous.customerIds.size()))
                .averageOrderValue(round2(aov))
                .averageOrderValuePrevious(round2(previousAov))
                .averageOrderValueChangePct(changePct(aov, previousAov))
                .build();

        double grandSales = totalSales;
        List<BranchSalesDto> branchWiseSales = branchTotals.values().stream()
                .sorted(Comparator.comparingDouble((BranchTotals b) -> b.sales).reversed())
                .map(b -> BranchSalesDto.builder()
                        .branchId(b.id)
                        .name(b.name)
                        .sales(round2(b.sales))
                        .sharePct(grandSales <= 0 ? 0.0 : round2(b.sales / grandSales * 100))
                        .build())
                .toList();

        List<BranchPerformanceDto> branchPerformance = branchTotals.values().stream()
                .sorted(Comparator.comparingDouble((BranchTotals b) -> b.sales).reversed())
                .map(b -> BranchPerformanceDto.builder()
                        .branchId(b.id)
                        .name(b.name)
                        .sales(round2(b.sales))
                        .orders(b.orders)
                        .averageOrderValue(round2(b.orders == 0 ? 0.0 : b.sales / b.orders))
                        .build())
                .toList();

        List<RecentSaleDto> recentSales = recent.stream()
                .map(order -> RecentSaleDto.builder()
                        .orderId(order.getId())
                        .branchId(order.getBranch() != null ? order.getBranch().getId() : null)
                        .branchName(order.getBranch() != null ? order.getBranch().getName() : "Unassigned")
                        .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
                        .amount(round2(order.getTotalAmount() != null ? order.getTotalAmount() : 0.0))
                        .status(order.getStatus() != null ? order.getStatus().name() : OrderStatus.PENDING.name())
                        .build())
                .toList();

        List<TopProductDto> topProducts = productTotals.values().stream()
                .sorted(Comparator.comparingDouble((ProductTotals p) -> p.revenue).reversed())
                .limit(TOP_PRODUCTS_LIMIT)
                .map(p -> TopProductDto.builder()
                        .productId(p.id)
                        .name(p.name)
                        .image(p.image)
                        .unitsSold((long) p.units)
                        .revenue(round2(p.revenue))
                        .build())
                .toList();

        long statusTotal = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        List<OrderStatusDto> orderStatus = statusCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(entry -> OrderStatusDto.builder()
                        .status(entry.getKey().name())
                        .label(humanize(entry.getKey().name()))
                        .count(entry.getValue())
                        .percentage(statusTotal == 0
                                ? 0.0
                                : round2(entry.getValue() * 100.0 / statusTotal))
                        .build())
                .toList();

        return StoreDashboardDto.builder()
                .storeId(storeId)
                .branchId(branchId)
                .period(period.key)
                .granularity(granularity.name())
                .granularityOptions(optionsFor(defaultGranularity))
                .startDate(start.toString())
                .endDate(today.toString())
                .generatedAt(LocalDateTime.now().toString())
                .metrics(metrics)
                .labels(labels)
                .sparklineLabels(sparklineLabels)
                .sparklines(sparklines)
                .salesTrend(salesTrend)
                .branchWiseSales(branchWiseSales)
                .branchPerformance(branchPerformance)
                .recentSales(recentSales)
                .topProducts(topProducts)
                .orderStatus(orderStatus)
                .build();
    }

    // ------------------------------------------------------------------
    // Bucketing
    // ------------------------------------------------------------------

    private List<String> buildLabels(LocalDate start, LocalDate endExclusive, Granularity granularity) {
        List<String> labels = new ArrayList<>();
        switch (granularity) {
            case HOURLY -> {
                for (LocalDate day = start; day.isBefore(endExclusive); day = day.plusDays(1)) {
                    for (int hour = 0; hour < 24; hour++) {
                        labels.add(String.format("%02d:00", hour));
                    }
                }
            }
            case DAILY -> {
                for (LocalDate day = start; day.isBefore(endExclusive); day = day.plusDays(1)) {
                    labels.add(DAY_LABEL.format(day));
                }
            }
            case WEEKLY -> {
                for (LocalDate day = start; day.isBefore(endExclusive); day = day.plusDays(7)) {
                    labels.add(DAY_LABEL.format(day));
                }
            }
            case MONTHLY -> {
                LocalDate month = start.withDayOfMonth(1);
                LocalDate last = endExclusive.minusDays(1).withDayOfMonth(1);
                while (!month.isAfter(last)) {
                    labels.add(MONTH_LABEL.format(month));
                    month = month.plusMonths(1);
                }
            }
        }
        if (labels.isEmpty()) {
            labels.add(DAY_LABEL.format(start));
        }
        return labels;
    }

    private int bucketIndex(LocalDateTime createdAt, LocalDate start, Granularity granularity, int size) {
        if (createdAt == null || size <= 0) {
            return 0;
        }
        LocalDate date = createdAt.toLocalDate();
        int index = switch (granularity) {
            case HOURLY -> (int) ChronoUnit.DAYS.between(start, date) * 24 + createdAt.getHour();
            case DAILY -> (int) ChronoUnit.DAYS.between(start, date);
            case WEEKLY -> (int) (ChronoUnit.DAYS.between(start, date) / 7);
            case MONTHLY -> (int) ChronoUnit.MONTHS.between(
                    start.withDayOfMonth(1), date.withDayOfMonth(1));
        };
        return Math.min(Math.max(index, 0), size - 1);
    }

    /** Keeps a requested bucketing sane for the window it has to cover. */
    private Granularity clamp(Granularity granularity, long days) {
        if (granularity == Granularity.HOURLY) {
            return days <= 1 ? Granularity.HOURLY : Granularity.DAILY;
        }
        if (granularity == Granularity.DAILY && days > 120) {
            return Granularity.WEEKLY;
        }
        if (granularity == Granularity.WEEKLY && days > 400) {
            return Granularity.MONTHLY;
        }
        return granularity;
    }

    private List<String> optionsFor(Granularity defaultGranularity) {
        return defaultGranularity == Granularity.HOURLY
                ? List.of("HOURLY", "DAILY")
                : List.of("DAILY", "WEEKLY", "MONTHLY");
    }

    // ------------------------------------------------------------------
    // Branch-only extras
    // ------------------------------------------------------------------

    /** Payment split, assigned staff and refunds inside the window. */
    private void enrichBranch(StoreDashboardDto dto, Long branchId, Window window, List<Order> orders) {
        Map<PaymentType, Double> byMethod = new EnumMap<>(PaymentType.class);
        double gross = 0.0;
        for (Order order : orders) {
            PaymentType type = order.getPaymentType() != null ? order.getPaymentType() : PaymentType.CASH;
            double amount = order.getTotalAmount() != null ? order.getTotalAmount() : 0.0;
            byMethod.merge(type, amount, Double::sum);
            gross += amount;
        }
        final double grossTotal = gross;
        dto.setPaymentBreakdown(byMethod.entrySet().stream()
                .sorted(Map.Entry.<PaymentType, Double>comparingByValue().reversed())
                .map(entry -> PaymentSplitDto.builder()
                        .method(entry.getKey().name())
                        .amount(round2(entry.getValue()))
                        .percentage(grossTotal > 0 ? round2(entry.getValue() / grossTotal * 100.0) : 0.0)
                        .build())
                .toList());

        dto.setActiveStaff((long) userRepository.findByBranchId(branchId).size());

        LocalDateTime from = window.rangeStart;
        LocalDateTime to = window.rangeEnd;
        List<Refund> refunds = refundRepository.findByBranchId(branchId).stream()
                .filter(refund -> refund.getCreatedAt() != null
                        && !refund.getCreatedAt().isBefore(from)
                        && refund.getCreatedAt().isBefore(to))
                .toList();
        dto.setTotalRefunds((long) refunds.size());
        dto.setRefundAmount(round2(refunds.stream()
                .mapToDouble(refund -> refund.getAmount() != null ? refund.getAmount() : 0.0)
                .sum()));
    }

    /** Resolved window plus the bucketing applied to it, shared by both dashboard scopes. */
    private record Window(
            Period period,
            LocalDate start,
            LocalDate endExclusive,
            long days,
            Granularity granularity,
            Granularity defaultGranularity,
            List<String> labels,
            List<String> sparklineLabels,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            LocalDateTime previousStart) {
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Period-over-period change. Returns null when there is nothing to compare against,
     * so the UI can say "no prior data" instead of inventing a percentage.
     */
    private Double changePct(double current, double previous) {
        if (previous <= 0) {
            return null;
        }
        return round2((current - previous) / previous * 100.0);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static List<Double> toList(double[] values) {
        List<Double> result = new ArrayList<>(values.length);
        for (double value : values) {
            result.add(round2(value));
        }
        return result;
    }

    private static String humanize(String enumName) {
        String spaced = enumName.replace('_', ' ').toLowerCase(Locale.ROOT);
        return spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
    }

    private enum Granularity {
        HOURLY, DAILY, WEEKLY, MONTHLY;

        static Granularity resolve(String raw, Granularity fallback) {
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            try {
                return valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return fallback;
            }
        }
    }

    private enum Period {
        TODAY("TODAY", Granularity.HOURLY),
        D7("7D", Granularity.DAILY),
        D30("30D", Granularity.DAILY),
        M3("3M", Granularity.DAILY),
        Y1("1Y", Granularity.MONTHLY);

        final String key;
        final Granularity defaultGranularity;

        Period(String key, Granularity defaultGranularity) {
            this.key = key;
            this.defaultGranularity = defaultGranularity;
        }

        static Period resolve(String raw) {
            if (raw == null || raw.isBlank()) {
                return D7;
            }
            return switch (raw.trim().toUpperCase(Locale.ROOT).replace(" ", "")) {
                case "TODAY", "1D" -> TODAY;
                case "30D" -> D30;
                case "3M", "90D" -> M3;
                case "1Y", "365D" -> Y1;
                default -> D7;
            };
        }

        LocalDate start(LocalDate today) {
            return switch (this) {
                case TODAY -> today;
                case D7 -> today.minusDays(6);
                case D30 -> today.minusDays(29);
                case M3 -> today.minusDays(89);
                case Y1 -> today.minusDays(364);
            };
        }
    }

    private static final class Totals {
        double sales;
        long orders;
        final Set<Long> customerIds = new HashSet<>();

        void add(double amount, Long customerId) {
            sales += amount;
            orders++;
            if (customerId != null) {
                customerIds.add(customerId);
            }
        }
    }

    private static final class BranchTotals {
        final Long id;
        final String name;
        double sales;
        long orders;

        BranchTotals(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        void add(double amount) {
            sales += amount;
            orders++;
        }
    }

    private static final class ProductTotals {
        final Long id;
        final String name;
        final String image;
        long units;
        double revenue;

        ProductTotals(Long id, String name, String image) {
            this.id = id;
            this.name = name;
            this.image = image;
        }

        void add(int quantity, double amount) {
            units += quantity;
            revenue += amount;
        }
    }
}