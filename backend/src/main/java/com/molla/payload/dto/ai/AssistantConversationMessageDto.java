package com.molla.payload.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AssistantConversationMessageDto(
        @NotBlank
        @Pattern(regexp = "user|assistant")
        String role,

        @NotBlank
        @Size(max = 4000)
        String content
) {
}
