package com.jf.PetApp.application.academy.dto;

import java.util.List;

public record AcademyDomainView(
        String id,
        String title,
        String description,
        String iconKey,
        int order,
        List<String> schoolIds) {
}
