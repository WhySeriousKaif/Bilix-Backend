package com.molla.payload.dto.ai;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AiTransactionDto(
        Long transactionId,
        Long storeId,
        Long branchId,
        LocalDateTime occurredAt,
        String paymentType,
        BigDecimal totalAmount,
        List<AiOrderItemDto> items


) {
}
