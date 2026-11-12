package com.molla.payload.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record AiAssistantRequest(
        @JsonProperty("store_id")
        Long storeId,

        @JsonProperty("branch_id")
        Long branchId,

        String message,

        List<AssistantConversationMessageDto> conversation,

        @JsonProperty("assistant_scope")
        String assistantScope,

        @JsonProperty("system_context")
        Map<String, Object> systemContext
) {
}
