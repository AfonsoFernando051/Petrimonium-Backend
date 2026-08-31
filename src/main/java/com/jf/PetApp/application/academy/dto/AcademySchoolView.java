package com.jf.PetApp.application.academy.dto;

import java.util.List;

public record AcademySchoolView(
        String id,
        String domainId,
        String title,
        String description,
        String iconKey,
        int order,
        List<String> prerequisites,
        boolean contentAvailable) {
}
