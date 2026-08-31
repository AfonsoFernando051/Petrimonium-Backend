package com.jf.PetApp.application.gamification.mission;

import org.junit.jupiter.api.Test;

import com.jf.PetApp.core.domain.gamification.MissionPeriod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionDefinitionTest {

    private static final MissionDefinition MISSION = new MissionDefinition(
            "COMPLETE_3_LESSONS", MissionPeriod.DAILY, 3, 50, MissionContext::lessonsCompletedInPeriod);

    @Test
    void accessorsReturnConstructedValues() {
        assertEquals("COMPLETE_3_LESSONS", MISSION.code());
        assertEquals(MissionPeriod.DAILY, MISSION.period());
        assertEquals(3, MISSION.targetCount());
        assertEquals(50, MISSION.xpReward());
    }

    @Test
    void progressFor_WithProgressBelowTarget_ReturnsTheRawProgress() {
        assertEquals(1, MISSION.progressFor(new MissionContext(1, 0)));
    }

    @Test
    void progressFor_WithProgressAtTarget_ReturnsTheTarget() {
        assertEquals(3, MISSION.progressFor(new MissionContext(3, 0)));
    }

    @Test
    void progressFor_WithProgressAboveTarget_IsCappedAtTheTarget() {
        // A user who completed 7 lessons in the period must still show 3/3, not 7/3.
        assertEquals(3, MISSION.progressFor(new MissionContext(7, 0)));
    }

    @Test
    void progressFor_WithZeroProgress_ReturnsZero() {
        assertEquals(0, MISSION.progressFor(new MissionContext(0, 0)));
    }

    @Test
    void isComplete_WithProgressBelowTarget_ReturnsFalse() {
        assertFalse(MISSION.isComplete(new MissionContext(2, 0)));
    }

    @Test
    void isComplete_WithProgressExactlyAtTarget_ReturnsTrue() {
        assertTrue(MISSION.isComplete(new MissionContext(3, 0)));
    }

    @Test
    void isComplete_WithProgressAboveTarget_ReturnsTrue() {
        assertTrue(MISSION.isComplete(new MissionContext(10, 0)));
    }

    @Test
    void progressFunction_CanBeBasedOnModulesInsteadOfLessons() {
        MissionDefinition moduleMission = new MissionDefinition(
                "COMPLETE_1_MODULE", MissionPeriod.WEEKLY, 1, 100, MissionContext::modulesCompletedInPeriod);

        assertTrue(moduleMission.isComplete(new MissionContext(0, 1)));
        assertFalse(moduleMission.isComplete(new MissionContext(99, 0)));
    }
}
