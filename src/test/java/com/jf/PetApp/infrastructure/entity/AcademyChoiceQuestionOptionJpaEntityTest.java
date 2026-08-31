package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademyChoiceQuestionOptionJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AcademyChoiceQuestionOptionJpaEntity entity = new AcademyChoiceQuestionOptionJpaEntity();

        entity.setStepId(10L);
        entity.setPosition(2);

        assertThat(entity.getStepId()).isEqualTo(10L);
        assertThat(entity.getPosition()).isEqualTo(2);
        assertThat(entity.getId()).isNull();
    }
}
