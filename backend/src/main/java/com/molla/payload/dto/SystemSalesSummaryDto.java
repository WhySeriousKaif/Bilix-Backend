package com.molla.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSalesSummaryDto {
    private Double totalSales;
    private Long ordersToday;
    private Long totalRefunds;
    private Double refundAmount;
    private Double netSales;
    private List<NamedAmountDto> paymentMethods;
    private List<IdSalesDto> storeSales;
    private List<IdSalesDto> branchSales;
    private List<NamedAmountDto> dailySales;

    @Data
    @AllArgsConstructor
    public static class NamedAmountDto {
        private String name;
        private Double amount;
    }

    @Data
    @AllArgsConstructor
    public static class IdSalesDto {
        private Long id;
        private Double sales;
        private Long orders;
    }
}
