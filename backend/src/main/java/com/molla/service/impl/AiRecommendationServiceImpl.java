package com.molla.service.impl;

import com.molla.payload.dto.ai.AiRecommendationRequest;
import com.molla.payload.dto.ai.AiServiceRecommendationResponse;
import com.molla.payload.dto.ai.RecommendationResponseDto;
import com.molla.service.AiRecommendationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private final RestClient aiRestClient;


    @Override
    public RecommendationResponseDto recommend(
            AiRecommendationRequest request
    ) {
        try {
            AiServiceRecommendationResponse response =
                    aiRestClient
                            .post()
                            .uri("/recommendations")
                            .body(request)
                            .retrieve()
                            .body(
                                    AiServiceRecommendationResponse.class
                            );

            if (response == null) {
                log.warn(
                        "AI service returned an empty response "
                                + "for store {}",
                        request.storeId()
                );

                return RecommendationResponseDto.unavailable(
                        request.storeId()
                );
            }

            return RecommendationResponseDto.available(
                    response
            );
        }
        catch (RestClientException exception) {
            log.warn(
                    "AI recommendation request failed "
                            + "for store {}: {}",
                    request.storeId(),
                    exception.getMessage()
            );

            return RecommendationResponseDto.unavailable(
                    request.storeId()
            );
        }
    }
}