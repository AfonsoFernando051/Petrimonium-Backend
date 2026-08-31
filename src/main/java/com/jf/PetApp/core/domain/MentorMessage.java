package com.jf.PetApp.core.domain;

import java.time.Instant;

/**
 * One turn within a {@link MentorConversation}. {@code role} is
 * {@code "user"} or {@code "mentor"}, matching {@code MentorTurnDTO}'s
 * existing convention for shaping Gemini's chat history.
 */
public record MentorMessage(
        Long id,
        Long conversationId,
        String role,
        String text,
        Instant createdAt
) {
}
