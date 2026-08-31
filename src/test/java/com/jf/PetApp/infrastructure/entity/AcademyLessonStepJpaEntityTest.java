package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademyLessonStepJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AcademyLessonStepJpaEntity entity = new AcademyLessonStepJpaEntity();

        entity.setLessonId("money_fundamentals_what_is_money");
        entity.setStepOrder(1);
        entity.setStepType("CHOICE_QUESTION");
        entity.setFraming("MICRO_EXERCISE");
        entity.setCorrectOptionIndex(1);

        assertThat(entity.getLessonId()).isEqualTo("money_fundamentals_what_is_money");
        assertThat(entity.getStepOrder()).isEqualTo(1);
        assertThat(entity.getStepType()).isEqualTo("CHOICE_QUESTION");
        assertThat(entity.getFraming()).isEqualTo("MICRO_EXERCISE");
        assertThat(entity.getCorrectOptionIndex()).isEqualTo(1);
    }
}
