package com.jf.PetApp.application.mentor.dto;

import java.time.Instant;

public record ConversationSummaryDTO(Long id, String title, Instant updatedAt, String lastMessagePreview) {
}
