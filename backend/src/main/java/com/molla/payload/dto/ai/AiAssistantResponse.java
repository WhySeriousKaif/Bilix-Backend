package com.molla.payload.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiAssistantResponse(
        String answer,

        @JsonProperty("tools_used")
        List<String> toolsUsed,

        List<AiAssistantSourceDto> sources,

        @JsonProperty("request_id")
        String requestId
) {
}
