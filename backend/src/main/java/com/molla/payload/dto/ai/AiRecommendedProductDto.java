package com.molla.payload.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record AiRecommendedProductDto(

        @JsonProperty("product_id")
        Long productId,

        BigDecimal score,

        BigDecimal confidence,

        BigDecimal lift,

        String reason
) {
}