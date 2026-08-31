package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademyLessonTranslationJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AcademyLessonTranslationJpaEntity entity = new AcademyLessonTranslationJpaEntity();

        entity.setLessonId("money_fundamentals_what_is_money");
        entity.setLang("en");
        entity.setTitle("What Is Money?");
        entity.setLearningObjective("Explain what money is.");

        assertThat(entity.getLessonId()).isEqualTo("money_fundamentals_what_is_money");
        assertThat(entity.getLang()).isEqualTo("en");
        assertThat(entity.getTitle()).isEqualTo("What Is Money?");
        assertThat(entity.getLearningObjective()).isEqualTo("Explain what money is.");
    }
}
