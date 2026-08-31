package com.jf.PetApp.infrastructure.seed.academy.model;

import java.util.List;

/**
 * A lesson step's text in one language — a superset of fields across all 4
 * step types (explanation/example use title+body, choice_question uses
 * prompt+options+explanation, summary uses title+takeaways). Whichever
 * fields don't apply to a given step's type are simply absent from the JSON
 * and deserialize to null/empty here.
 */
public record StepTextDto(
        String title,
        String body,
        String prompt,
        List<String> options,
        String explanation,
        List<String> takeaways) {
}
