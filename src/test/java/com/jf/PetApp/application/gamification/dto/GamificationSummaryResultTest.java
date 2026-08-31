package com.jf.PetApp.application.gamification.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GamificationSummaryResultTest {

    @Test
    void accessorsReturnConstructedValues() {
        GamificationSummaryResult result = new GamificationSummaryResult(150, 3, 50, 100, 5, 12);

        assertEquals(150, result.totalXp());
        assertEquals(3, result.level());
        assertEquals(50, result.xpIntoLevel());
        assertEquals(100, result.xpForNextLevel());
        assertEquals(5, result.currentStreak());
        assertEquals(12, result.longestStreak());
    }
}
