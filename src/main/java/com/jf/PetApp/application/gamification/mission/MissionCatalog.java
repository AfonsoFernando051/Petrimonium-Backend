package com.jf.PetApp.application.gamification.mission;

import java.util.List;

import com.jf.PetApp.core.domain.gamification.MissionPeriod;

/**
 * The fixed, permanent mission catalog — every entry is conditioned purely
 * on learning/practice signals (lesson and module completions already
 * recorded in the {@code xp_events} ledger), never on portfolio wealth,
 * profit, or risk-taking. Unlike {@code AchievementCatalog}, there is no
 * historical "wealth-tied" mission to migrate away from — this catalog
 * starts learning-only (see DECISION-014's resolution note in
 * DECISIONS.md), closing the gap the achievement catalog only closed after
 * the fact.
 */
public final class MissionCatalog {

    private MissionCatalog() {
    }

    public static final List<MissionDefinition> DEFINITIONS = List.of(
            new MissionDefinition("daily_complete_lesson", MissionPeriod.DAILY, 1, 30,
                    MissionContext::lessonsCompletedInPeriod),
            new MissionDefinition("daily_complete_two_lessons", MissionPeriod.DAILY, 2, 60,
                    MissionContext::lessonsCompletedInPeriod),
            new MissionDefinition("weekly_complete_three_lessons", MissionPeriod.WEEKLY, 3, 100,
                    MissionContext::lessonsCompletedInPeriod),
            new MissionDefinition("weekly_complete_module", MissionPeriod.WEEKLY, 1, 150,
                    MissionContext::modulesCompletedInPeriod));
}
