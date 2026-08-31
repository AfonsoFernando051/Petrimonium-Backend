package com.jf.PetApp.application.gamification.port;

import java.time.Instant;
import java.util.Set;

import com.jf.PetApp.core.domain.gamification.XpEventType;

/**
 * Application-layer boundary for the XP ledger. Use cases and services
 * depend on this port, never on Spring Data or JPA entities directly.
 */
public interface XpEventRepositoryPort {

    boolean existsByUserIdAndEventTypeAndSourceId(Long userId, XpEventType eventType, String sourceId);

    void save(Long userId, XpEventType eventType, int amount, String sourceId);

    /** Sum of every XP event's amount for the user — the total XP source of truth. */
    int sumAmountByUserId(Long userId);

    /**
     * How many {@code eventType} events this user has in
     * {@code [fromInclusive, toExclusive)} — the mission system's sole
     * source of learning-progress signals (see {@code MissionContext}).
     */
    int countByUserIdAndEventTypeAndCreatedAtBetween(
            Long userId, XpEventType eventType, Instant fromInclusive, Instant toExclusive);

    /**
     * Every {@code sourceId} this user has an {@code eventType} event for —
     * e.g. the set of Financial Lab simulator ids they've completed, since
     * the {@code xp_events} row for {@code SIMULATOR_COMPLETED} *is* the
     * completion record (no separate progress table, unlike lessons).
     */
    Set<String> sourceIdsByUserIdAndEventType(Long userId, XpEventType eventType);
}
