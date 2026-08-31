package com.jf.PetApp.application.mentor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MentorChatRequest(
    @NotBlank @Size(max = 2000) String message,
    Long conversationId,
    @Valid MentorClientContextDTO context
) {
}
