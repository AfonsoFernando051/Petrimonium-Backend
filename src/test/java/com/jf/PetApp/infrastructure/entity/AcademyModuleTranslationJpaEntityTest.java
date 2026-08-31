package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademyModuleTranslationJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AcademyModuleTranslationJpaEntity entity = new AcademyModuleTranslationJpaEntity();

        entity.setModuleId("money_fundamentals");
        entity.setLang("en");
        entity.setTitle("Money Fundamentals");
        entity.setDescription("Description");

        assertThat(entity.getModuleId()).isEqualTo("money_fundamentals");
        assertThat(entity.getLang()).isEqualTo("en");
        assertThat(entity.getTitle()).isEqualTo("Money Fundamentals");
        assertThat(entity.getDescription()).isEqualTo("Description");
    }
}
