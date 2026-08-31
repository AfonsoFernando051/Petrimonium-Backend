package com.jf.PetApp.application.gamification.dto;

import com.jf.PetApp.core.domain.gamification.MissionPeriod;

public record MissionStatusDTO(
        String code,
        MissionPeriod period,
        String periodKey,
        int progress,
        int target,
        int xpReward,
        boolean completed) {
}
