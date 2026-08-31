package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademyModulePrerequisiteJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AcademyModulePrerequisiteJpaEntity entity = new AcademyModulePrerequisiteJpaEntity();

        entity.setModuleId("investing_basics");
        entity.setPrerequisiteModuleId("money_fundamentals");

        assertThat(entity.getModuleId()).isEqualTo("investing_basics");
        assertThat(entity.getPrerequisiteModuleId()).isEqualTo("money_fundamentals");
    }
}
