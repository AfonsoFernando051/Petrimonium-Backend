package com.jf.PetApp.core.domain;

import com.jf.PetApp.core.domain.enums.RealPortfolioSyncStatus;

import java.time.Instant;

/**
 * One audited attempt to sync a user's real portfolio from an external
 * provider (e.g. B3) — always recorded, even when {@link #status} is
 * {@link RealPortfolioSyncStatus#DISABLED} (no legitimate credentials
 * configured). {@code idempotencyKey} is unique per {@code (userEmail,
 * provider)}: retrying the same logical sync request returns this same
 * logged outcome instead of re-running it.
 */
public record RealPortfolioSyncLog(
        Long id,
        String userEmail,
        String provider,
        String idempotencyKey,
        RealPortfolioSyncStatus status,
        Instant startedAt,
        Instant finishedAt,
        String message
) {
}
