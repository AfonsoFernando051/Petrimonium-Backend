package com.jf.PetApp.application.gamification.usecase;

import com.jf.PetApp.application.gamification.dto.GamificationSummaryResult;

public interface GetGamificationSummaryUseCase {
    GamificationSummaryResult execute(String userEmail);
}
