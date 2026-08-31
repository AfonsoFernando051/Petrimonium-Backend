package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademyLessonStepTakeawayJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AcademyLessonStepTakeawayJpaEntity entity = new AcademyLessonStepTakeawayJpaEntity();

        entity.setStepId(4L);
        entity.setPosition(0);

        assertThat(entity.getStepId()).isEqualTo(4L);
        assertThat(entity.getPosition()).isZero();
    }
}
