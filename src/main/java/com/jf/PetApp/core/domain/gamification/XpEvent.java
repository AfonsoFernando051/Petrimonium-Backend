package com.jf.PetApp.core.domain.gamification;

import java.time.Instant;

/**
 * A single, immutable XP grant. The XP ledger's total is always
 * {@code SUM(amount)} over these — never a single mutable counter — so it
 * stays auditable and safe to recompute from scratch at any time.
 *
 * {@code (userId, eventType, sourceId)} is the idempotency key: granting the
 * same event twice (e.g. a retried lesson-completion request) must never
 * create a second row — see {@code XpLedgerService}.
 */
public record XpEvent(Long id, Long userId, XpEventType eventType, int amount, String sourceId, Instant createdAt) {
}
