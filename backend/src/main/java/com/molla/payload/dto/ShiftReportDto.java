package com.molla.payload.dto;

import com.molla.model.PaymentSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftReportDto {
    private Long id;
    private LocalDateTime shiftStart;
    private LocalDateTime shiftEnd;
    private Double totalSales;
    private Double totalRefunds;
    private Double netSale;
    private Integer totalOrders;
    private UserDto cashier;
    private Long cashierId;
    private BranchDto branch;
    private Long branchId;
    private List<PaymentSummary> paymentSummaries;
    private List<ProductDto> topSellingProducts;
    private List<OrderDto> recentOrders;
    private List<RefundDto> refunds;
    private LocalDateTime createdAt;
}

