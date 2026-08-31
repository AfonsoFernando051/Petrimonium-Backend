package com.jf.PetApp.infrastructure.seed.academy.model;

/**
 * A lesson's title in one language, plus its optional learning objective
 * ("what the learner can DO after this lesson" — see DECISION-025). Not yet
 * authored for every lesson; {@code null} where absent from the JSON.
 */
public record LessonTextDto(String title, String learningObjective) {
}
