package com.jf.PetApp.application.gamification.port;

import java.time.Instant;
import java.util.Map;

/**
 * Application-layer boundary for persisted achievement unlocks. Use cases
 * depend on this port, never on Spring Data or JPA entities directly.
 */
public interface AchievementRepositoryPort {

    boolean isUnlocked(Long userId, String achievementCode);

    /** No-op if the achievement is already unlocked for this user (idempotent). */
    void unlock(Long userId, String achievementCode, int xpAwarded);

    /** Every achievement this user has unlocked, mapped to when it was unlocked. */
    Map<String, Instant> unlockedWithTimestamps(Long userId);

    /** Sum of XP awarded across every achievement this user has unlocked. */
    int totalXpFor(Long userId);
}
