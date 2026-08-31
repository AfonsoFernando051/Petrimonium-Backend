package com.jf.PetApp.application.gamification.mission;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;

import com.jf.PetApp.core.domain.gamification.MissionPeriod;

/**
 * Turns a mission's period type and a reference date into the current
 * period's stable key (e.g. {@code "2026-08-19"} for a daily mission,
 * {@code "2026-W34"} for a weekly one, ISO week rules) plus the
 * {@link Instant} window used to count qualifying {@code xp_events} rows.
 * Pure and stateless — the same reference date always yields the same
 * window, so evaluating a mission is safe to call as often as needed.
 */
public final class MissionPeriodKeyCalculator {

    private MissionPeriodKeyCalculator() {
    }

    public record PeriodWindow(String periodKey, Instant startInclusive, Instant endExclusive) {
    }

    public static PeriodWindow windowFor(MissionPeriod period, LocalDate referenceDate) {
        ZoneId zone = ZoneId.systemDefault();
        if (period == MissionPeriod.DAILY) {
            Instant start = referenceDate.atStartOfDay(zone).toInstant();
            Instant end = referenceDate.plusDays(1).atStartOfDay(zone).toInstant();
            return new PeriodWindow(referenceDate.toString(), start, end);
        }

        WeekFields isoWeek = WeekFields.ISO;
        int weekBasedYear = referenceDate.get(isoWeek.weekBasedYear());
        int weekOfYear = referenceDate.get(isoWeek.weekOfWeekBasedYear());
        String periodKey = String.format("%d-W%02d", weekBasedYear, weekOfYear);

        LocalDate weekStart = referenceDate.with(isoWeek.dayOfWeek(), 1);
        Instant start = weekStart.atStartOfDay(zone).toInstant();
        Instant end = weekStart.plusWeeks(1).atStartOfDay(zone).toInstant();
        return new PeriodWindow(periodKey, start, end);
    }
}
