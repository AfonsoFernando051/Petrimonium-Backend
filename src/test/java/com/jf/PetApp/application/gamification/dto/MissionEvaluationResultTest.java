package com.jf.PetApp.application.gamification.dto;

import com.jf.PetApp.core.domain.gamification.MissionPeriod;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissionEvaluationResultTest {

    @Test
    void accessorsReturnConstructedValues() {
        MissionStatusDTO mission = new MissionStatusDTO(
                "COMPLETE_3_LESSONS", MissionPeriod.DAILY, "2026-08-23", 3, 3, 50, true);
        MissionEvaluationResult result =
                new MissionEvaluationResult(List.of(mission), Set.of("COMPLETE_3_LESSONS"), 50);

        assertEquals(List.of(mission), result.missions());
        assertEquals(Set.of("COMPLETE_3_LESSONS"), result.newlyCompletedCodes());
        assertEquals(50, result.missionXpTotal());
    }
}
