package com.jf.PetApp.application.gamification.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AchievementEvaluationResultTest {

    @Test
    void accessorsReturnConstructedValues() {
        Instant unlockedAt = Instant.parse("2026-08-23T10:00:00Z");
        AchievementEvaluationResult result = new AchievementEvaluationResult(
                Map.of("FIRST_ASSET", unlockedAt), Set.of("FIRST_ASSET"), 100);

        assertEquals(Map.of("FIRST_ASSET", unlockedAt), result.unlockedAt());
        assertEquals(Set.of("FIRST_ASSET"), result.newlyUnlockedCodes());
        assertEquals(100, result.achievementXpTotal());
    }
}
