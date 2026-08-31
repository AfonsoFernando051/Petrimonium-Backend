package com.jf.PetApp.core.domain.gamification;

/**
 * How often a mission's instance resets. A {@code DAILY} mission's period
 * key is the calendar date; a {@code WEEKLY} mission's is the ISO
 * week-based year and week number — see
 * {@code application.gamification.mission.MissionPeriodKeyCalculator}.
 */
public enum MissionPeriod {
    DAILY,
    WEEKLY
}
