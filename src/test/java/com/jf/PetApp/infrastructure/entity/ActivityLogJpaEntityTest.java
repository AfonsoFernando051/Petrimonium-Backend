package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityLogJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        ActivityLogJpaEntity entity = new ActivityLogJpaEntity();
        LocalDate activityDate = LocalDate.of(2024, 1, 1);

        entity.setId(1L);
        entity.setUserId(42L);
        entity.setActivityDate(activityDate);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUserId()).isEqualTo(42L);
        assertThat(entity.getActivityDate()).isEqualTo(activityDate);
    }
}
