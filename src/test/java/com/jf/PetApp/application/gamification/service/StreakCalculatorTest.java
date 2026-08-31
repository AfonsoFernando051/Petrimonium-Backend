package com.jf.PetApp.application.gamification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 16);

    @Test
    void currentStreak_NoActivity_IsZero() {
        assertEquals(0, StreakCalculator.currentStreak(List.of(), TODAY));
    }

    @Test
    void currentStreak_ActiveToday_CountsConsecutiveDaysEndingToday() {
        List<LocalDate> datesDesc = List.of(
                TODAY, TODAY.minusDays(1), TODAY.minusDays(2));

        assertEquals(3, StreakCalculator.currentStreak(datesDesc, TODAY));
    }

    @Test
    void currentStreak_ActiveYesterdayNotToday_StillCountsViaGracePeriod() {
        List<LocalDate> datesDesc = List.of(TODAY.minusDays(1), TODAY.minusDays(2));

        assertEquals(2, StreakCalculator.currentStreak(datesDesc, TODAY));
    }

    @Test
    void currentStreak_GapBeforeYesterday_ResetsToZero() {
        List<LocalDate> datesDesc = List.of(TODAY.minusDays(3), TODAY.minusDays(4));

        assertEquals(0, StreakCalculator.currentStreak(datesDesc, TODAY));
    }

    @Test
    void currentStreak_StopsCountingAtFirstGapInHistory() {
        List<LocalDate> datesDesc = List.of(
                TODAY, TODAY.minusDays(1), TODAY.minusDays(4), TODAY.minusDays(5));

        assertEquals(2, StreakCalculator.currentStreak(datesDesc, TODAY));
    }

    @Test
    void longestStreak_NoActivity_IsZero() {
        assertEquals(0, StreakCalculator.longestStreak(List.of()));
    }

    @Test
    void longestStreak_FindsLongestRunEvenIfNotCurrent() {
        // A 3-day run far in the past, then a gap, then a lone recent day.
        List<LocalDate> datesDesc = List.of(
                TODAY.minusDays(10),
                TODAY.minusDays(20), TODAY.minusDays(21), TODAY.minusDays(22));

        assertEquals(3, StreakCalculator.longestStreak(datesDesc));
    }
}
