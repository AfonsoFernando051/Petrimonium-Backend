package com.jf.PetApp.application.investment.port;

import com.jf.PetApp.core.domain.RealPortfolioSyncLog;
import com.jf.PetApp.core.domain.enums.RealPortfolioSyncStatus;

import java.time.Instant;
import java.util.Optional;

/**
 * Persistence boundary for the real-portfolio sync audit trail. Every sync
 * attempt is recorded here — including {@code DISABLED} ones — so "did we
 * already try this?" and "what happened last time?" are always answerable
 * without re-calling the external provider.
 */
public interface RealPortfolioSyncLogRepositoryPort {

    Optional<RealPortfolioSyncLog> findByUserEmailAndProviderAndIdempotencyKey(
            String userEmail, String provider, String idempotencyKey);

    RealPortfolioSyncLog save(
            String userEmail,
            String provider,
            String idempotencyKey,
            RealPortfolioSyncStatus status,
            Instant startedAt,
            String message);
}
