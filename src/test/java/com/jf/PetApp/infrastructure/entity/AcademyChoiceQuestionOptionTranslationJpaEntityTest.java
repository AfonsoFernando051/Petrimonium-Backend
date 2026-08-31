package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademyChoiceQuestionOptionTranslationJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AcademyChoiceQuestionOptionTranslationJpaEntity entity = new AcademyChoiceQuestionOptionTranslationJpaEntity();

        entity.setOptionId(1L);
        entity.setLang("en");
        entity.setOptionText("Option A");

        assertThat(entity.getOptionId()).isEqualTo(1L);
        assertThat(entity.getLang()).isEqualTo("en");
        assertThat(entity.getOptionText()).isEqualTo("Option A");
    }
}
