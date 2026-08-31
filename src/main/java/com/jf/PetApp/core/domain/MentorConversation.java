package com.jf.PetApp.core.domain;

import java.time.Instant;

/**
 * A single Mentor chat thread belonging to one user. {@code title} is
 * {@code null} until the first message is exchanged, at which point it's
 * auto-derived from that message. {@code appContext} is the
 * {@link com.jf.PetApp.core.domain.enums.AppContextEnum#claimValue()} the
 * conversation was created under ({@code null} for one created before this
 * field existed) — every read/write path filters on it so a Wallet session
 * can never surface an Academy thread or vice versa.
 */
public record MentorConversation(
        Long id,
        String userEmail,
        String title,
        Instant createdAt,
        Instant updatedAt,
        String appContext
) {
}
