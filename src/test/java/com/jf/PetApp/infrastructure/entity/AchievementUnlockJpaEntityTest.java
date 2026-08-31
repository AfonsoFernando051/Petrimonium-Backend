package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementUnlockJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        AchievementUnlockJpaEntity entity = new AchievementUnlockJpaEntity();
        Instant unlockedAt = Instant.parse("2024-01-01T00:00:00Z");

        entity.setId(1L);
        entity.setUserId(42L);
        entity.setAchievementCode("first_lesson");
        entity.setXpAwarded(30);
        entity.setUnlockedAt(unlockedAt);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUserId()).isEqualTo(42L);
        assertThat(entity.getAchievementCode()).isEqualTo("first_lesson");
        assertThat(entity.getXpAwarded()).isEqualTo(30);
        assertThat(entity.getUnlockedAt()).isEqualTo(unlockedAt);
    }
}
