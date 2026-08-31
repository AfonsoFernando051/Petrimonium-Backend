package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademyDomainTranslationJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AcademyDomainTranslationJpaEntity entity = new AcademyDomainTranslationJpaEntity();

        entity.setDomainId("financial_education");
        entity.setLang("en");
        entity.setTitle("Financial Education");
        entity.setDescription("Description");

        assertThat(entity.getDomainId()).isEqualTo("financial_education");
        assertThat(entity.getLang()).isEqualTo("en");
        assertThat(entity.getTitle()).isEqualTo("Financial Education");
        assertThat(entity.getDescription()).isEqualTo("Description");
    }
}
