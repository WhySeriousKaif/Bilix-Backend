package com.molla.payload.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiServiceRecommendationResponse(

        @JsonProperty("store_id")
        Long storeId,

        List<AiRecommendedProductDto> recommendations,

        @JsonProperty("model_version")
        String modelVersion
) {
}