package com.jf.PetApp.infrastructure.entity;

import com.jf.PetApp.core.domain.gamification.XpEventType;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class XpEventJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        XpEventJpaEntity entity = new XpEventJpaEntity();
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");

        entity.setId(1L);
        entity.setUserId(42L);
        entity.setEventType(XpEventType.LESSON_COMPLETED);
        entity.setAmount(50);
        entity.setSourceId("lesson_1");
        entity.setCreatedAt(createdAt);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUserId()).isEqualTo(42L);
        assertThat(entity.getEventType()).isEqualTo(XpEventType.LESSON_COMPLETED);
        assertThat(entity.getAmount()).isEqualTo(50);
        assertThat(entity.getSourceId()).isEqualTo("lesson_1");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    }
}
