package com.molla.payload.dto.ai;

import java.math.BigDecimal;

public record AiOrderItemDto(
        Long productId,
        String productSku,
        String productName,
        Long categoryId,
        String categoryName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
