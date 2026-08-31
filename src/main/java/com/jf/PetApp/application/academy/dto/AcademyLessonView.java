package com.jf.PetApp.application.academy.dto;

import java.util.List;

public record AcademyLessonView(
        String id,
        String moduleId,
        String title,
        String learningObjective,
        String competency,
        Integer estimatedMinutes,
        int order,
        int xpReward,
        List<String> portfolioConcepts,
        List<AcademyLessonStepView> steps) {
}
