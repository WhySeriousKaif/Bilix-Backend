package com.molla.payload.dto.ai;

public record AiAssistantSourceDto(
        String type,
        String label,
        String document,
        String section
) {
}
