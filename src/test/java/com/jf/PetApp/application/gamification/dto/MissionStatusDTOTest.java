package com.jf.PetApp.application.gamification.dto;

import com.jf.PetApp.core.domain.gamification.MissionPeriod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionStatusDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        MissionStatusDTO dto = new MissionStatusDTO(
                "COMPLETE_3_LESSONS", MissionPeriod.DAILY, "2026-08-23", 2, 3, 50, false);

        assertEquals("COMPLETE_3_LESSONS", dto.code());
        assertEquals(MissionPeriod.DAILY, dto.period());
        assertEquals("2026-08-23", dto.periodKey());
        assertEquals(2, dto.progress());
        assertEquals(3, dto.target());
        assertEquals(50, dto.xpReward());
        assertEquals(false, dto.completed());
    }

    @Test
    void accessorsReturnConstructedValues_WhenCompleted() {
        MissionStatusDTO dto = new MissionStatusDTO(
                "COMPLETE_3_LESSONS", MissionPeriod.WEEKLY, "2026-W34", 3, 3, 50, true);

        assertTrue(dto.completed());
        assertEquals(MissionPeriod.WEEKLY, dto.period());
    }
}
