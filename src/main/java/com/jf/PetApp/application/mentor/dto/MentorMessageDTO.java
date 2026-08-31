package com.jf.PetApp.application.mentor.dto;

import java.time.Instant;

public record MentorMessageDTO(Long id, String role, String text, Instant createdAt) {
}
