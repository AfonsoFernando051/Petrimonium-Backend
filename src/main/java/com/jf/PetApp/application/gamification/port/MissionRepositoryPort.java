package com.jf.PetApp.application.gamification.port;

/**
 * Application-layer boundary for persisted mission completions. Use cases
 * depend on this port, never on Spring Data or JPA entities directly.
 */
public interface MissionRepositoryPort {

    boolean isCompleted(Long userId, String missionCode, String periodKey);

    /** No-op if already completed for this (missionCode, periodKey) — idempotent. */
    void complete(Long userId, String missionCode, String periodKey, int xpAwarded);

    /** Sum of XP awarded across every mission period this user has completed, all-time. */
    int totalXpFor(Long userId);
}
