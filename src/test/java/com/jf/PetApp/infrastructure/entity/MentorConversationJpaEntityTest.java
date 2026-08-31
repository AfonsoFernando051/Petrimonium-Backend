package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MentorConversationJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        MentorConversationJpaEntity entity = new MentorConversationJpaEntity();
        UserJpaEntity user = new UserJpaEntity();
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2024-01-02T00:00:00Z");

        entity.setId(1L);
        entity.setUser(user);
        entity.setTitle("Investing questions");
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        entity.setAppContext("wallet");

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUser()).isSameAs(user);
        assertThat(entity.getTitle()).isEqualTo("Investing questions");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(entity.getAppContext()).isEqualTo("wallet");
    }
}
