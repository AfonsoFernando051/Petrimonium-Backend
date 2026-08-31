package com.jf.PetApp.infrastructure.seed.academy.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StepSeedDtoTest {

    @Test
    void construction_RoundTripsChoiceQuestionFields() {
        Map<String, StepTextDto> translations = Map.of(
                "en", new StepTextDto(null, null, "What is X?", null, "Because Y.", null));

        StepSeedDto dto = new StepSeedDto("choice_question", 2, "framing text", 1, translations);

        assertThat(dto.type()).isEqualTo("choice_question");
        assertThat(dto.order()).isEqualTo(2);
        assertThat(dto.framing()).isEqualTo("framing text");
        assertThat(dto.correctIndex()).isEqualTo(1);
        assertThat(dto.translations()).isEqualTo(translations);
    }

    @Test
    void construction_AllowsNullFramingAndCorrectIndex() {
        StepSeedDto dto = new StepSeedDto("explanation", 1, null, null, Map.of());

        assertThat(dto.type()).isEqualTo("explanation");
        assertThat(dto.framing()).isNull();
        assertThat(dto.correctIndex()).isNull();
    }
}
