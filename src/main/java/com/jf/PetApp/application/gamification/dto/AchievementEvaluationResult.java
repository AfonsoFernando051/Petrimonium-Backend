package com.jf.PetApp.application.gamification.dto;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record AchievementEvaluationResult(
        Map<String, Instant> unlockedAt,
        Set<String> newlyUnlockedCodes,
        int achievementXpTotal) {
}
