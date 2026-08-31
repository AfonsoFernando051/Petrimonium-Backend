package com.jf.PetApp.application.learning.usecase;

import com.jf.PetApp.application.learning.dto.LessonCompletionResult;

public interface CompleteLessonUseCase {
    LessonCompletionResult execute(String userEmail, String lessonId, boolean perfectFirstTry);
}
