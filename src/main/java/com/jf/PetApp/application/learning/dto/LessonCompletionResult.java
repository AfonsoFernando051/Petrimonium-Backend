package com.jf.PetApp.application.learning.dto;

public record LessonCompletionResult(
        String lessonId,
        boolean alreadyCompleted,
        int xpAwarded,
        boolean moduleCompleted,
        int moduleXpAwarded,
        int totalXp,
        int level,
        int xpIntoLevel,
        int xpForNextLevel) {
}
