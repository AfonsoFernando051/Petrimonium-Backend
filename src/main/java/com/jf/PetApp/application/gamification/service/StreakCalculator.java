package com.jf.PetApp.application.gamification.service;

import java.time.LocalDate;
import java.util.List;

/**
 * Pure functions deriving engagement-streak facts from a user's recorded
 * activity dates. Mirrors {@code LevelCalculator}'s style: no framework
 * dependencies, always recomputed from the real activity log, never a
 * stored/cached counter.
 */
public final class StreakCalculator {

    private StreakCalculator() {
    }

    /**
     * The current consecutive-day streak, allowing a one-day grace period —
     * a streak counts as "current" if the most recent activity was today or
     * yesterday, so it doesn't reset the instant midnight passes. Returns 0
     * if the most recent activity is older than yesterday, or there is none.
     *
     * @param datesDesc distinct activity dates, sorted most-recent first
     */
    public static int currentStreak(List<LocalDate> datesDesc, LocalDate today) {
        if (datesDesc.isEmpty()) {
            return 0;
        }
        LocalDate mostRecent = datesDesc.get(0);
        long gapFromToday = java.time.temporal.ChronoUnit.DAYS.between(mostRecent, today);
        if (gapFromToday > 1) {
            return 0;
        }

        int streak = 1;
        LocalDate expectedPrevious = mostRecent.minusDays(1);
        for (int i = 1; i < datesDesc.size(); i++) {
            LocalDate date = datesDesc.get(i);
            if (date.equals(expectedPrevious)) {
                streak++;
                expectedPrevious = date.minusDays(1);
            } else if (date.isBefore(expectedPrevious)) {
                break;
            }
        }
        return streak;
    }

    /**
     * The longest run of consecutive dates anywhere in the history.
     *
     * @param datesDesc distinct activity dates, sorted most-recent first
     */
    public static int longestStreak(List<LocalDate> datesDesc) {
        if (datesDesc.isEmpty()) {
            return 0;
        }
        int longest = 1;
        int current = 1;
        for (int i = 1; i < datesDesc.size(); i++) {
            LocalDate previous = datesDesc.get(i - 1);
            LocalDate date = datesDesc.get(i);
            if (previous.minusDays(1).equals(date)) {
                current++;
            } else {
                current = 1;
            }
            longest = Math.max(longest, current);
        }
        return longest;
    }
}
