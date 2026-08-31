package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademyDomainJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AcademyDomainJpaEntity entity = new AcademyDomainJpaEntity();

        entity.setDomainId("financial_education");
        entity.setOrderIndex(1);
        entity.setIconKey("savings_outlined");

        assertThat(entity.getDomainId()).isEqualTo("financial_education");
        assertThat(entity.getOrderIndex()).isEqualTo(1);
        assertThat(entity.getIconKey()).isEqualTo("savings_outlined");
    }
}
