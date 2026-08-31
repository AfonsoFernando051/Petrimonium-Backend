package com.jf.PetApp.application.gamification.service;

import com.jf.PetApp.core.domain.gamification.PlayerLevel;

/**
 * Derives a user's level from total accumulated XP. Mirrors
 * {@code petapp_mobile/lib/features/game/domain/services/level_calculator.dart}
 * exactly (same triangular progression, same 50-XP step) so the server and
 * the Flutter client never disagree about what level a given XP total means.
 */
public final class LevelCalculator {

    private static final int XP_STEP = 50;

    private LevelCalculator() {
    }

    private static int totalXpForLevel(int level) {
        return (XP_STEP * (level - 1) * level) / 2;
    }

    public static PlayerLevel fromXp(int xp) {
        int level = 1;
        while (totalXpForLevel(level + 1) <= xp) {
            level++;
        }
        int base = totalXpForLevel(level);
        return new PlayerLevel(level, xp - base, totalXpForLevel(level + 1) - base);
    }
}
