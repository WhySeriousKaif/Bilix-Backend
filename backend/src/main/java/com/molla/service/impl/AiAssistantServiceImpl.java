package com.molla.service.impl;

import com.molla.payload.dto.ai.AiAssistantRequest;
import com.molla.payload.dto.ai.AiAssistantResponse;
import com.molla.payload.dto.ai.AssistantResponseDto;
import com.molla.service.AiAssistantService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantServiceImpl implements AiAssistantService {

    private final RestClient aiRestClient;

    @Override
    public AssistantResponseDto chat(
            AiAssistantRequest request,
            String userAuthorization
    ) {
        try {
            AiAssistantResponse response = aiRestClient
                    .post()
                    .uri("/assistant/chat")
                    .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                    .body(request)
                    .retrieve()
                    .body(AiAssistantResponse.class);

            if (response == null) {
                log.warn(
                        "AI assistant returned an empty response for store {} branch {}",
                        request.storeId(),
                        request.branchId()
                );
                return AssistantResponseDto.unavailable();
            }

            return AssistantResponseDto.available(response);
        }
        catch (RestClientException exception) {
            log.warn(
                    "AI assistant request failed for store {} branch {}: {}",
                    request.storeId(),
                    request.branchId(),
                    exception.getMessage()
            );
            return AssistantResponseDto.unavailable();
        }
    }
}
