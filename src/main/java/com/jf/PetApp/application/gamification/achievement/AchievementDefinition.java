package com.jf.PetApp.application.gamification.achievement;

import java.util.function.Predicate;

/**
 * A single achievement: its stable {@code code} (the idempotency key stored
 * in {@code achievement_unlocks}), the XP it grants the first time it's
 * unlocked, and the real-data condition that must hold for it to unlock.
 * Title/description/icon are display-only concerns and stay client-side
 * (see {@code petapp_mobile/lib/features/portfolio/domain/services/achievement_catalog.dart},
 * which this list is kept in sync with).
 */
public record AchievementDefinition(String code, int xpReward, Predicate<AchievementContext> qualifies) {
}
