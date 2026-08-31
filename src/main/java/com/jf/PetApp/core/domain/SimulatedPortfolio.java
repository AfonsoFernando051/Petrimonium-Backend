package com.jf.PetApp.core.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Academy's fictitious wallet — one per user, entirely separate from
 * {@link Finance}/{@link Investment} (the real_portfolio context owned by
 * Wallet). Never backed by real money and never shared storage, entities,
 * or endpoints with the real portfolio — see docs/BACKEND_MODULE_PLAN.md §2.
 */
public record SimulatedPortfolio(
        Long id,
        String userEmail,
        BigDecimal virtualBalance,
        BigDecimal initialBalance,
        String currency,
        Instant resetAt,
        Instant createdAt,
        Instant updatedAt
) {
}
