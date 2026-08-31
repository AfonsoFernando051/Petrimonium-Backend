package com.jf.PetApp.core.domain.gamification;

/**
 * A user's current level and progress toward the next one, derived from
 * total accumulated XP — never a hardcoded number. Mirrors
 * {@code petapp_mobile/lib/features/game/domain/entities/player_level.dart}
 * so the server and the Flutter client always agree.
 */
public record PlayerLevel(int level, int xpIntoLevel, int xpForNextLevel) {

    /** Progress toward the next level, clamped to [0.0, 1.0]. */
    public double progress() {
        if (xpForNextLevel == 0) {
            return 1.0;
        }
        return Math.min(1.0, Math.max(0.0, (double) xpIntoLevel / xpForNextLevel));
    }
}
