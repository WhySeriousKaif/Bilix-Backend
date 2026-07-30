package com.molla.model;

import com.molla.domain.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSummary {
    private PaymentType type;
    private Double totalAmount;
    private Integer transactionCount;
    private Double percentage;
}
