package com.molla.payload.dto.ai;

import java.util.List;

public record AssistantResponseDto(
        String answer,
        List<String> toolsUsed,
        List<AiAssistantSourceDto> sources,
        String requestId,
        boolean available,
        String message
) {
    public static AssistantResponseDto available(AiAssistantResponse response) {
        return new AssistantResponseDto(
                response.answer(),
                response.toolsUsed() == null ? List.of() : response.toolsUsed(),
                response.sources() == null ? List.of() : response.sources(),
                response.requestId(),
                true,
                "Assistant response generated successfully"
        );
    }

    public static AssistantResponseDto unavailable() {
        return new AssistantResponseDto(
                "The assistant is temporarily unavailable. Please try again later.",
                List.of(),
                List.of(),
                null,
                false,
                "AI assistant is temporarily unavailable"
        );
    }
}
