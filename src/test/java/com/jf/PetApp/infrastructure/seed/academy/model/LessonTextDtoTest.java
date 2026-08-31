package com.jf.PetApp.infrastructure.seed.academy.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LessonTextDtoTest {

    @Test
    void construction_RoundTripsFields() {
        LessonTextDto dto = new LessonTextDto("What is compound interest?", "Calculate compound interest.");

        assertThat(dto.title()).isEqualTo("What is compound interest?");
        assertThat(dto.learningObjective()).isEqualTo("Calculate compound interest.");
    }

    @Test
    void construction_AllowsNullLearningObjective() {
        LessonTextDto dto = new LessonTextDto("What is compound interest?", null);

        assertThat(dto.title()).isEqualTo("What is compound interest?");
        assertThat(dto.learningObjective()).isNull();
    }
}
