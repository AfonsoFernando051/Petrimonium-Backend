package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcademySchoolPrerequisiteJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AcademySchoolPrerequisiteJpaEntity entity = new AcademySchoolPrerequisiteJpaEntity();

        entity.setSchoolId("investing_101");
        entity.setPrerequisiteSchoolId("money_fundamentals");

        assertThat(entity.getSchoolId()).isEqualTo("investing_101");
        assertThat(entity.getPrerequisiteSchoolId()).isEqualTo("money_fundamentals");
        assertThat(entity.getId()).isNull();
    }
}
