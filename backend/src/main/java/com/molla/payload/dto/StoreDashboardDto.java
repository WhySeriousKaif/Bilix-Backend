package com.molla.payload.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Everything the store-admin dashboard renders, computed from real order/branch/product
 * rows for a single requested period. Nothing here is hardcoded — every number is derived
 * from the orders belonging to the requested store (or, for the branch view, one branch).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDashboardDto {

    private Long storeId;
    /** Set only by the branch-scoped endpoint; null for the store view. */
    private Long branchId;
    /** Requested period key: TODAY | 7D | 30D | 3M | 1Y */
    private String period;
    /** Bucketing actually applied after clamping: HOURLY | DAILY | WEEKLY | MONTHLY */
    private String granularity;
    /** Granularity choices the dashboard should offer for this period. */
    private List<String> granularityOptions;
    /** Inclusive ISO start date and exclusive ISO end date of the measured window. */
    private String startDate;
    private String endDate;
    /** ISO date-time now, so the client can render "x mins ago" without trusting its own clock. */
    private String generatedAt;

    private MetricsDto metrics;

    /** Bucket labels for the trend charts, aligned with every series below. */
    private List<String> labels;
    /** Bucket labels for the metric-card sparklines (always the period's default bucketing). */
    private List<String> sparklineLabels;
    /** metric key (totalSales/totalOrders/uniqueCustomers/averageOrderValue) -> per-bucket values */
    private Map<String, List<Double>> sparklines;

    /** Sales Trend / Sales Overview: total plus per-branch sales in every bucket. */
    private List<TrendPointDto> salesTrend;

    private List<BranchSalesDto> branchWiseSales;
    private List<BranchPerformanceDto> branchPerformance;
    private List<RecentSaleDto> recentSales;
    private List<TopProductDto> topProducts;
    private List<OrderStatusDto> orderStatus;

    /* ---- branch-scoped extras (null for the store view) ---- */
    private List<PaymentSplitDto> paymentBreakdown;
    private Long activeStaff;
    private Long totalRefunds;
    private Double refundAmount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsDto {
        private Double totalSales;
        private Double totalSalesPrevious;
        private Double totalSalesChangePct;

        private Long totalOrders;
        private Long totalOrdersPrevious;
        private Double totalOrdersChangePct;

        private Long uniqueCustomers;
        private Long uniqueCustomersPrevious;
        private Double uniqueCustomersChangePct;

        private Double averageOrderValue;
        private Double averageOrderValuePrevious;
        private Double averageOrderValueChangePct;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPointDto {
        private String label;
        private Double total;
        /** branch name -> sales inside this bucket */
        private Map<String, Double> branches;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchSalesDto {
        private Long branchId;
        private String name;
        private Double sales;
        private Double sharePct;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchPerformanceDto {
        private Long branchId;
        private String name;
        private Double sales;
        private Long orders;
        private Double averageOrderValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentSaleDto {
        private Long orderId;
        private Long branchId;
        private String branchName;
        private String createdAt;
        private Double amount;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductDto {
        private Long productId;
        private String name;
        private String image;
        private Long unitsSold;
        private Double revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderStatusDto {
        private String status;
        private String label;
        private Long count;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSplitDto {
        private String method;
        private Double amount;
        private Double percentage;
    }
}