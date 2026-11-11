package com.molla.service.impl;

import com.molla.payload.dto.ai.AiRecommendationRequest;
import com.molla.payload.dto.ai.RecommendationResponseDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AiRecommendationServiceImplTest {

    private MockRestServiceServer server;

    private AiRecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(
                                "http://localhost:8000"
                        );

        server = MockRestServiceServer
                .bindTo(builder)
                .build();

        service = new AiRecommendationServiceImpl(
                builder.build()
        );
    }

    @Test
    void returnsRecommendationsFromAiService() {
        server.expect(
                        once(),
                        requestTo(
                                "http://localhost:8000"
                                        + "/recommendations"
                        )
                )
                .andExpect(method(
                        org.springframework.http.HttpMethod.POST
                ))
                .andExpect(content().json("""
                        {
                          "store_id": 1,
                          "product_ids": [3],
                          "top_k": 3
                        }
                        """))
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "store_id": 1,
                                  "recommendations": [
                                    {
                                      "product_id": 9,
                                      "score": 2.82,
                                      "confidence": 0.94,
                                      "lift": 2.99,
                                      "reason": "association_rule"
                                    }
                                  ],
                                  "model_version": "1.0.0"
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        RecommendationResponseDto response =
                service.recommend(
                        new AiRecommendationRequest(
                                1L,
                                List.of(3L),
                                3
                        )
                );

        assertThat(response.available()).isTrue();

        assertThat(response.recommendations())
                .hasSize(1);

        assertThat(
                response.recommendations()
                        .get(0)
                        .productId()
        ).isEqualTo(9L);

        server.verify();
    }

    @Test
    void returnsUnavailableWhenAiServiceFails() {
        server.expect(
                        once(),
                        requestTo(
                                "http://localhost:8000"
                                        + "/recommendations"
                        )
                )
                .andRespond(
                        withServerError()
                );

        RecommendationResponseDto response =
                service.recommend(
                        new AiRecommendationRequest(
                                1L,
                                List.of(3L),
                                3
                        )
                );

        assertThat(response.available()).isFalse();
        assertThat(response.recommendations()).isEmpty();

        server.verify();
    }
}