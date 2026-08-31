package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LessonProgressJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        LessonProgressJpaEntity entity = new LessonProgressJpaEntity();
        Instant completedAt = Instant.parse("2024-01-01T00:00:00Z");

        entity.setId(1L);
        entity.setUserId(42L);
        entity.setLessonId("lesson_1");
        entity.setCompletedAt(completedAt);
        entity.setPerfectFirstTry(true);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUserId()).isEqualTo(42L);
        assertThat(entity.getLessonId()).isEqualTo("lesson_1");
        assertThat(entity.getCompletedAt()).isEqualTo(completedAt);
        assertThat(entity.isPerfectFirstTry()).isTrue();
    }
}
