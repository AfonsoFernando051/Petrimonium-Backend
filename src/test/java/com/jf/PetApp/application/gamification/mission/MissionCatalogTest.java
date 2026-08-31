package com.jf.PetApp.application.gamification.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * DECISION-014 regression coverage for the mission system: every mission
 * must be conditioned purely on learning/practice signals — never on
 * portfolio wealth, profit, or risk-taking. Unlike the achievement catalog,
 * missions never had a wealth-tied entry to begin with; this test locks
 * that in so a future addition can't accidentally introduce one.
 */
class MissionCatalogTest {

    @Test
    void everyMissionHasAPositiveXpRewardAndTarget() {
        for (MissionDefinition definition : MissionCatalog.DEFINITIONS) {
            assertTrue(definition.xpReward() > 0, definition.code() + " should grant positive XP");
            assertTrue(definition.targetCount() > 0, definition.code() + " should require positive progress");
        }
    }

    @Test
    void everyMissionConditionIsDrivenOnlyByLessonOrModuleCompletionCounts() {
        // Maxing out every non-learning-shaped field of MissionContext is
        // impossible by construction — MissionContext only has two fields,
        // both learning-completion counts — so this test instead proves no
        // mission can be satisfied by an empty period (zero completions).
        MissionContext emptyPeriod = new MissionContext(0, 0);

        for (MissionDefinition definition : MissionCatalog.DEFINITIONS) {
            assertFalse(definition.isComplete(emptyPeriod),
                    definition.code() + " must not be completable with zero learning activity in the period");
        }
    }

    @Test
    void dailyMissionXpRewardsAreSmallerThanWeeklyOnes() {
        // Same unit (XP) across both periods, unlike targetCount which mixes
        // lesson-counts and module-counts — a meaningful "daily is the
        // smaller ask" comparison has to be in XP, not raw progress counts.
        int maxDailyXp = MissionCatalog.DEFINITIONS.stream()
                .filter(d -> d.period() == com.jf.PetApp.core.domain.gamification.MissionPeriod.DAILY)
                .mapToInt(MissionDefinition::xpReward)
                .max()
                .orElseThrow();
        int minWeeklyXp = MissionCatalog.DEFINITIONS.stream()
                .filter(d -> d.period() == com.jf.PetApp.core.domain.gamification.MissionPeriod.WEEKLY)
                .mapToInt(MissionDefinition::xpReward)
                .min()
                .orElseThrow();

        assertTrue(maxDailyXp <= minWeeklyXp,
                "a daily mission's XP reward shouldn't exceed a weekly mission's — daily missions are the smaller ask");
    }

    @Test
    void everyMissionCodeIsUnique() {
        long distinctCodes = MissionCatalog.DEFINITIONS.stream().map(MissionDefinition::code).distinct().count();

        assertEquals(MissionCatalog.DEFINITIONS.size(), distinctCodes);
    }
}
