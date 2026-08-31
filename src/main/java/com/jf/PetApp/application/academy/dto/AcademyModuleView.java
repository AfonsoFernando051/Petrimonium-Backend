package com.jf.PetApp.application.academy.dto;

import java.util.List;

public record AcademyModuleView(
        String id,
        String schoolId,
        String title,
        String description,
        String iconKey,
        String difficulty,
        int order,
        List<String> lessonIds,
        List<String> prerequisites,
        boolean contentAvailable) {
}
