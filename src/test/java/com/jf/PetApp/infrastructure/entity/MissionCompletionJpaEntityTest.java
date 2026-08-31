package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MissionCompletionJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        MissionCompletionJpaEntity entity = new MissionCompletionJpaEntity();
        Instant completedAt = Instant.parse("2024-01-01T00:00:00Z");

        entity.setId(1L);
        entity.setUserId(42L);
        entity.setMissionCode("daily_login");
        entity.setPeriodKey("2024-01-01");
        entity.setXpAwarded(20);
        entity.setCompletedAt(completedAt);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUserId()).isEqualTo(42L);
        assertThat(entity.getMissionCode()).isEqualTo("daily_login");
        assertThat(entity.getPeriodKey()).isEqualTo("2024-01-01");
        assertThat(entity.getXpAwarded()).isEqualTo(20);
        assertThat(entity.getCompletedAt()).isEqualTo(completedAt);
    }
}
