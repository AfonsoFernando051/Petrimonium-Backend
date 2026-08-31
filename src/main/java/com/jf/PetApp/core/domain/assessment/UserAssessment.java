package com.jf.PetApp.core.domain.assessment;

import java.util.List;

public record UserAssessment(
    Long userId,
    List<String> selectedOptionIds
) {}
