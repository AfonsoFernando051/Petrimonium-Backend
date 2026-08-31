package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademyLessonStepTranslationJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AcademyLessonStepTranslationJpaEntity entity = new AcademyLessonStepTranslationJpaEntity();

        entity.setStepId(1L);
        entity.setLang("en");
        entity.setTitle("Why?");
        entity.setBody("Body text");
        entity.setPrompt("Why?");
        entity.setExplanation("Because.");

        assertThat(entity.getStepId()).isEqualTo(1L);
        assertThat(entity.getLang()).isEqualTo("en");
        assertThat(entity.getTitle()).isEqualTo("Why?");
        assertThat(entity.getBody()).isEqualTo("Body text");
        assertThat(entity.getPrompt()).isEqualTo("Why?");
        assertThat(entity.getExplanation()).isEqualTo("Because.");
    }
}
