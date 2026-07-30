package com.molla.mapper;

import com.molla.model.Refund;
import com.molla.payload.dto.RefundDto;

public class RefundMapper {
    
    public static RefundDto toDto(Refund refund) {
        if (refund == null) {
            return null;
        }
        
        return RefundDto.builder()
            .id(refund.getId())
            .orderId(refund.getOrder() != null ? refund.getOrder().getId() : null)
            .reason(refund.getReason())
            .amount(refund.getAmount())
            .shiftReportId(refund.getShiftReport() != null ? refund.getShiftReport().getId() : null)
            .cashier(refund.getCashier() != null ? UserMapper.toDto(refund.getCashier()) : null)
            .branch(refund.getBranch() != null ? BranchMapper.toDto(refund.getBranch()) : null)
            .paymentType(refund.getPaymentType())
            .createdAt(refund.getCreatedAt())
            .build();
    }
}

