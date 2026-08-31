package com.jf.PetApp.application.learning.dto;

import java.util.Set;

public record LearningProgressResult(
        Set<String> completedLessonIds,
        Set<String> completedModuleIds,
        Set<String> perfectLessonIds,
        int totalXp,
        int level,
        int xpIntoLevel,
        int xpForNextLevel) {
}
