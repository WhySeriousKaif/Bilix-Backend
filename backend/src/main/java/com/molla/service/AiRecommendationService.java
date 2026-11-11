package com.molla.service;

import com.molla.payload.dto.ai.AiRecommendationRequest;
import com.molla.payload.dto.ai.RecommendationResponseDto;

public interface AiRecommendationService {

    RecommendationResponseDto recommend(
            AiRecommendationRequest request
    );
}