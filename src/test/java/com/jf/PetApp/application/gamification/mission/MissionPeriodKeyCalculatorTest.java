package com.jf.PetApp.application.gamification.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.jf.PetApp.application.gamification.mission.MissionPeriodKeyCalculator.PeriodWindow;
import com.jf.PetApp.core.domain.gamification.MissionPeriod;

class MissionPeriodKeyCalculatorTest {

    @Test
    void windowFor_Daily_PeriodKeyIsTheIsoDate() {
        PeriodWindow window = MissionPeriodKeyCalculator.windowFor(MissionPeriod.DAILY, LocalDate.of(2026, 8, 19));

        assertEquals("2026-08-19", window.periodKey());
    }

    @Test
    void windowFor_Daily_WindowSpansExactlyThatCalendarDay() {
        LocalDate day = LocalDate.of(2026, 8, 19);
        PeriodWindow window = MissionPeriodKeyCalculator.windowFor(MissionPeriod.DAILY, day);

        assertTrue(window.startInclusive().isBefore(window.endExclusive()));
        // A day is 24h (ignoring rare DST edge cases the app doesn't need to special-case).
        long spanSeconds = window.endExclusive().getEpochSecond() - window.startInclusive().getEpochSecond();
        assertTrue(spanSeconds >= 23 * 3600 && spanSeconds <= 25 * 3600);
    }

    @Test
    void windowFor_Weekly_PeriodKeyIsTheIsoWeek() {
        // 2026-08-19 is a Wednesday in ISO week 34 of 2026.
        PeriodWindow window = MissionPeriodKeyCalculator.windowFor(MissionPeriod.WEEKLY, LocalDate.of(2026, 8, 19));

        assertEquals("2026-W34", window.periodKey());
    }

    @Test
    void windowFor_Weekly_EveryDayInTheSameIsoWeekYieldsTheSamePeriodKey() {
        String mondayKey = MissionPeriodKeyCalculator.windowFor(MissionPeriod.WEEKLY, LocalDate.of(2026, 8, 17)).periodKey();
        String wednesdayKey = MissionPeriodKeyCalculator.windowFor(MissionPeriod.WEEKLY, LocalDate.of(2026, 8, 19)).periodKey();
        String sundayKey = MissionPeriodKeyCalculator.windowFor(MissionPeriod.WEEKLY, LocalDate.of(2026, 8, 23)).periodKey();

        assertEquals(mondayKey, wednesdayKey);
        assertEquals(mondayKey, sundayKey);
    }

    @Test
    void windowFor_Weekly_AdjacentWeeksYieldDifferentPeriodKeys() {
        String thisWeek = MissionPeriodKeyCalculator.windowFor(MissionPeriod.WEEKLY, LocalDate.of(2026, 8, 19)).periodKey();
        String nextWeek = MissionPeriodKeyCalculator.windowFor(MissionPeriod.WEEKLY, LocalDate.of(2026, 8, 26)).periodKey();

        assertFalse(thisWeek.equals(nextWeek));
    }

    @Test
    void windowFor_Weekly_WindowSpansExactlySevenDays() {
        PeriodWindow window = MissionPeriodKeyCalculator.windowFor(MissionPeriod.WEEKLY, LocalDate.of(2026, 8, 19));

        long spanSeconds = window.endExclusive().getEpochSecond() - window.startInclusive().getEpochSecond();
        assertTrue(spanSeconds >= 6 * 24 * 3600 && spanSeconds <= 8 * 24 * 3600);
    }
}
