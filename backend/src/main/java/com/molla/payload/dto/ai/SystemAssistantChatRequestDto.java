package com.molla.payload.dto.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SystemAssistantChatRequestDto(
        @NotBlank
        @Size(min = 2, max = 2000)
        String message,

        @Size(max = 10)
        List<@Valid AssistantConversationMessageDto> conversation
) {
}
