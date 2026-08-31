package com.jf.PetApp.application.gamification.mission;

/**
 * The real learning facts a {@link MissionDefinition} may condition on,
 * scoped to a single mission's current period window (see
 * {@link MissionPeriodKeyCalculator}). Deliberately limited to signals the
 * XP ledger already records ({@code xp_events}' {@code LESSON_COMPLETED}/
 * {@code MODULE_COMPLETED} counts) — missions can never be conditioned on
 * portfolio wealth, profit, or any other non-learning signal, by
 * construction (see DECISION-014 and its resolution note in DECISIONS.md).
 */
public record MissionContext(int lessonsCompletedInPeriod, int modulesCompletedInPeriod) {
}
