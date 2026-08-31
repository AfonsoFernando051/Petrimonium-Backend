package com.jf.PetApp.application.gamification.port;

import java.time.LocalDate;
import java.util.List;

/**
 * Application-layer boundary for the engagement-activity log. Use cases
 * depend on this port, never on Spring Data or JPA entities directly.
 */
public interface ActivityRepositoryPort {

    /** No-op if activity was already recorded for this user/date (idempotent). */
    void recordActivity(Long userId, LocalDate date);

    /** Distinct activity dates for this user, sorted most-recent first. */
    List<LocalDate> recentActivityDates(Long userId);
}
