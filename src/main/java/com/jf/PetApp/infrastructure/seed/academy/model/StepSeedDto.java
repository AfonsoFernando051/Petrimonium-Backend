package com.jf.PetApp.infrastructure.seed.academy.model;

import java.util.Map;

/**
 * One lesson step from a school content JSON file. {@code type} is
 * lowercase snake_case ("explanation" | "example" | "choice_question" |
 * "summary") as authored; the seeder uppercases it for the
 * {@code academy_lesson_steps.step_type} column. {@code framing} and
 * {@code correctIndex} are only present for {@code choice_question} steps.
 */
public record StepSeedDto(
        String type,
        int order,
        String framing,
        Integer correctIndex,
        Map<String, StepTextDto> translations) {
}
