package com.molla.payload.dto;

import com.molla.domain.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundDto {
    private Long id;
    private Long orderId;
    private String reason;
    private Double amount;
    private Long shiftReportId;
    private UserDto cashier;
    private BranchDto branch;
    private PaymentType paymentType;
    private LocalDateTime createdAt;
}

