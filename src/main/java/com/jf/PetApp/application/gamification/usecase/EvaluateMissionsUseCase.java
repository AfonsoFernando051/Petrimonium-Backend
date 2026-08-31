package com.jf.PetApp.application.gamification.usecase;

import com.jf.PetApp.application.gamification.dto.MissionEvaluationResult;

public interface EvaluateMissionsUseCase {
    MissionEvaluationResult execute(String userEmail);
}
