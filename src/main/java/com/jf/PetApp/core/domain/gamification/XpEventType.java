package com.jf.PetApp.core.domain.gamification;

/**
 * The kinds of XP-granting events the backend currently recognizes.
 * {@code SIMULATOR_COMPLETED} (DECISION-037) is the Financial Lab's XP
 * source — mission/achievement event types are added when those systems
 * move server-side.
 */
public enum XpEventType {
    LESSON_COMPLETED,
    MODULE_COMPLETED,
    SIMULATOR_COMPLETED
}
