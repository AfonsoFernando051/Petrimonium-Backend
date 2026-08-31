package com.jf.PetApp.application.academy.dto;

import java.util.List;

/**
 * One lesson step, already resolved to a single requested language. {@code
 * type} is lowercase snake_case ("explanation" | "example" |
 * "choice_question" | "summary"), mirroring the authored JSON's convention.
 * Fields that don't apply to {@code type} are simply null/empty — the same
 * discriminated-union shape used by the database and the JSON content.
 */
public record AcademyLessonStepView(
        String type,
        String title,
        String body,
        String framing,
        List<String> options,
        Integer correctIndex,
        String prompt,
        String explanation,
        List<String> takeaways) {
}
