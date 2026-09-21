package com.jf.PetApp.core.domain;

import java.time.Instant;

/**
 * Academy's simulation wallet — one per user, holding only the positions the
 * user registers (no starting cash or balance of any kind), entirely separate from
 * {@link Finance}/{@link Investment} (the real_portfolio context owned by
 * Wallet). Never backed by real money and never shared storage, entities,
 * or endpoints with the real portfolio — see docs/BACKEND_MODULE_PLAN.md §2.
 */
public record SimulatedPortfolio(
        Long id,
        String userEmail,
        String currency,
        Instant resetAt,
        Instant createdAt,
        Instant updatedAt
) {
}
