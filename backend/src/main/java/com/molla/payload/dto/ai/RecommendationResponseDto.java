package com.molla.payload.dto.ai;

import java.util.List;

public record RecommendationResponseDto(
        Long storeId,
        List<AiRecommendedProductDto> recommendations,
        String modelVersion,
        boolean available,
        String message
) {

    public static RecommendationResponseDto available(
            AiServiceRecommendationResponse response
    ) {
        return new RecommendationResponseDto(
                response.storeId(),
                response.recommendations(),
                response.modelVersion(),
                true,
                "Recommendations generated successfully"
        );
    }

    public static RecommendationResponseDto unavailable(
            Long storeId
    ) {
        return new RecommendationResponseDto(
                storeId,
                List.of(),
                null,
                false,
                "AI recommendations are temporarily unavailable"
        );
    }
}