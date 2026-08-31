package com.jf.PetApp.application.gamification.mission;

import java.util.function.ToIntFunction;

import com.jf.PetApp.core.domain.gamification.MissionPeriod;

/**
 * A single mission: its stable {@code code} (part of the idempotency key
 * stored in {@code mission_completions}), how often it resets, the XP it
 * grants once per period, and the real-data progress function/target that
 * determine when it's complete. Title/description are display-only and stay
 * client-side, mirroring {@link com.jf.PetApp.application.gamification.achievement.AchievementDefinition}.
 */
public record MissionDefinition(
        String code, MissionPeriod period, int targetCount, int xpReward, ToIntFunction<MissionContext> progress) {

    public int progressFor(MissionContext context) {
        return Math.min(progress.applyAsInt(context), targetCount);
    }

    public boolean isComplete(MissionContext context) {
        return progress.applyAsInt(context) >= targetCount;
    }
}
