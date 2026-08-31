package com.jf.PetApp.application.mentor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameConversationRequest(@NotBlank @Size(max = 255) String title) {
}
