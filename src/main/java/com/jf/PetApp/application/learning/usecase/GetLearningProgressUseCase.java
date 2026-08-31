package com.jf.PetApp.application.learning.usecase;

import com.jf.PetApp.application.learning.dto.LearningProgressResult;

public interface GetLearningProgressUseCase {
    LearningProgressResult execute(String userEmail);
}
