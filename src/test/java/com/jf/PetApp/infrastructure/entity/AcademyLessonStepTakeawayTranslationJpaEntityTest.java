package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademyLessonStepTakeawayTranslationJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AcademyLessonStepTakeawayTranslationJpaEntity entity = new AcademyLessonStepTakeawayTranslationJpaEntity();

        entity.setTakeawayId(1L);
        entity.setLang("en");
        entity.setTakeawayText("Diversify your investments.");

        assertThat(entity.getTakeawayId()).isEqualTo(1L);
        assertThat(entity.getLang()).isEqualTo("en");
        assertThat(entity.getTakeawayText()).isEqualTo("Diversify your investments.");
    }
}
