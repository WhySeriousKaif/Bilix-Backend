package com.molla.payload.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record AiRecommendationRequest(

        @NotNull
        @Positive
        @JsonProperty("store_id")
        Long storeId,

        @NotNull
        @JsonProperty("product_ids")
        List<@Positive Long> productIds,

        @NotNull
        @Min(1)
        @Max(10)
        @JsonProperty("top_k")
        Integer topK
) {
}