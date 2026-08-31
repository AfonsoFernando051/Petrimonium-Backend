package com.jf.PetApp.core.domain;

import java.time.Instant;

/**
 * A single Mentor chat thread belonging to one user. {@code title} is
 * {@code null} until the first message is exchanged, at which point it's
 * auto-derived from that message.
 */
public record MentorConversation(
        Long id,
        String userEmail,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
}
