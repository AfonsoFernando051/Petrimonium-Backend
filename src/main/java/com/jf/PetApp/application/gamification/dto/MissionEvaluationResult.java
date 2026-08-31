package com.jf.PetApp.application.gamification.dto;

import java.util.List;
import java.util.Set;

public record MissionEvaluationResult(
        List<MissionStatusDTO> missions, Set<String> newlyCompletedCodes, int missionXpTotal) {
}
