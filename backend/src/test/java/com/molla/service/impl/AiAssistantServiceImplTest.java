package com.molla.service.impl;

import com.molla.payload.dto.ai.AiAssistantRequest;
import com.molla.payload.dto.ai.AssistantConversationMessageDto;
import com.molla.payload.dto.ai.AssistantResponseDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiAssistantServiceImplTest {

    private MockRestServiceServer server;
    private AiAssistantServiceImpl service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://localhost:8000");

        server = MockRestServiceServer
                .bindTo(builder)
                .build();

        service = new AiAssistantServiceImpl(builder.build());
    }

    @Test
    void returnsGroundedAssistantResponse() {
        server.expect(
                        once(),
                        requestTo("http://localhost:8000/assistant/chat")
                )
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer current-user-token"))
                .andExpect(content().json("""
                        {
                          "store_id": 1,
                          "branch_id": 2,
                          "message": "Can opened food be returned?",
                          "conversation": [
                            {
                              "role": "user",
                              "content": "I bought food yesterday"
                            }
                          ]
                        }
                        """))
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "answer": "Opened food is not normally returnable unless defective.",
                                  "tools_used": ["search_policies"],
                                  "sources": [
                                    {
                                      "type": "policy",
                                      "label": "Refund Policy — Opened products",
                                      "document": "refund-policy.md",
                                      "section": "Opened products"
                                    }
                                  ],
                                  "request_id": "request-123"
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        AssistantResponseDto response = service.chat(
                new AiAssistantRequest(
                        1L,
                        2L,
                        "Can opened food be returned?",
                        List.of(
                                new AssistantConversationMessageDto(
                                        "user",
                                        "I bought food yesterday"
                                )
                        ),
                        "manager",
                        null
                ),
                "Bearer current-user-token"
        );

        assertThat(response.available()).isTrue();
        assertThat(response.toolsUsed()).containsExactly("search_policies");
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).document())
                .isEqualTo("refund-policy.md");
        assertThat(response.requestId()).isEqualTo("request-123");
        server.verify();
    }

    @Test
    void returnsUnavailableWhenAssistantFails() {
        server.expect(
                        once(),
                        requestTo("http://localhost:8000/assistant/chat")
                )
                .andRespond(withServerError());

        AssistantResponseDto response = service.chat(
                new AiAssistantRequest(
                        1L,
                        2L,
                        "What is the refund policy?",
                        List.of(),
                        "manager",
                        null
                ),
                "Bearer current-user-token"
        );

        assertThat(response.available()).isFalse();
        assertThat(response.toolsUsed()).isEmpty();
        assertThat(response.sources()).isEmpty();
        server.verify();
    }
}
