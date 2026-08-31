package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademySchoolJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AcademySchoolJpaEntity entity = new AcademySchoolJpaEntity();

        entity.setSchoolId("investing_101");
        entity.setDomainId("financial_education");
        entity.setOrderIndex(2);
        entity.setIconKey("school_outlined");
        entity.setContentAvailable(true);

        assertThat(entity.getSchoolId()).isEqualTo("investing_101");
        assertThat(entity.getDomainId()).isEqualTo("financial_education");
        assertThat(entity.getOrderIndex()).isEqualTo(2);
        assertThat(entity.getIconKey()).isEqualTo("school_outlined");
        assertThat(entity.isContentAvailable()).isTrue();
    }
}
