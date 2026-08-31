package com.jf.PetApp.infrastructure.seed.academy.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StepTextDtoTest {

    @Test
    void construction_RoundTripsExplanationFields() {
        StepTextDto dto = new StepTextDto("Title", "Body text", null, null, null, null);

        assertThat(dto.title()).isEqualTo("Title");
        assertThat(dto.body()).isEqualTo("Body text");
        assertThat(dto.prompt()).isNull();
        assertThat(dto.options()).isNull();
        assertThat(dto.explanation()).isNull();
        assertThat(dto.takeaways()).isNull();
    }

    @Test
    void construction_RoundTripsChoiceQuestionFields() {
        List<String> options = List.of("A", "B", "C");

        StepTextDto dto = new StepTextDto(null, null, "What is X?", options, "Because Y.", null);

        assertThat(dto.prompt()).isEqualTo("What is X?");
        assertThat(dto.options()).containsExactly("A", "B", "C");
        assertThat(dto.explanation()).isEqualTo("Because Y.");
    }

    @Test
    void construction_RoundTripsSummaryFields() {
        List<String> takeaways = List.of("Key point 1", "Key point 2");

        StepTextDto dto = new StepTextDto("Summary", null, null, null, null, takeaways);

        assertThat(dto.title()).isEqualTo("Summary");
        assertThat(dto.takeaways()).containsExactly("Key point 1", "Key point 2");
    }
}
