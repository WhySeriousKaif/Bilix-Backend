package com.molla.service;

import com.molla.payload.dto.ai.AiAssistantRequest;
import com.molla.payload.dto.ai.AssistantResponseDto;

public interface AiAssistantService {
    AssistantResponseDto chat(
            AiAssistantRequest request,
            String userAuthorization
    );
}
