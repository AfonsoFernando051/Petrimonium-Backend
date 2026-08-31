package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademySchoolTranslationJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AcademySchoolTranslationJpaEntity entity = new AcademySchoolTranslationJpaEntity();

        entity.setSchoolId("investing_101");
        entity.setLang("en");
        entity.setTitle("Investing 101");
        entity.setDescription("Learn the basics of investing.");

        assertThat(entity.getSchoolId()).isEqualTo("investing_101");
        assertThat(entity.getLang()).isEqualTo("en");
        assertThat(entity.getTitle()).isEqualTo("Investing 101");
        assertThat(entity.getDescription()).isEqualTo("Learn the basics of investing.");
        assertThat(entity.getId()).isNull();
    }
}
