package com.jf.PetApp.application.gamification.usecase;

import com.jf.PetApp.application.gamification.dto.AchievementEvaluationResult;

public interface EvaluateAchievementsUseCase {
    AchievementEvaluationResult execute(String userEmail);
}
