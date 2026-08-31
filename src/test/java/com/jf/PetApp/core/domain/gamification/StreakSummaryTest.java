package com.jf.PetApp.core.domain.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * StreakSummary is a plain record with no custom behavior — this confirms
 * construction and accessors.
 */
class StreakSummaryTest {

    @Test
    void accessors_ReturnConstructedValues() {
        StreakSummary summary = new StreakSummary(3, 10);

        assertEquals(3, summary.currentStreak());
        assertEquals(10, summary.longestStreak());
    }
}
