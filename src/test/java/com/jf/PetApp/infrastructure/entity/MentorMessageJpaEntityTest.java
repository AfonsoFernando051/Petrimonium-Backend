package com.jf.PetApp.infrastructure.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MentorMessageJpaEntityTest {

    @Test
    void settersAndGetters_RoundTripFields() {
        MentorMessageJpaEntity entity = new MentorMessageJpaEntity();
        MentorConversationJpaEntity conversation = new MentorConversationJpaEntity();
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");

        entity.setId(1L);
        entity.setConversation(conversation);
        entity.setRole("user");
        entity.setContent("What is compound interest?");
        entity.setCreatedAt(createdAt);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getConversation()).isSameAs(conversation);
        assertThat(entity.getRole()).isEqualTo("user");
        assertThat(entity.getContent()).isEqualTo("What is compound interest?");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    }
}
