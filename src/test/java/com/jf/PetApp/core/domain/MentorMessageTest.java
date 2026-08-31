package com.jf.PetApp.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/**
 * MentorMessage is a plain record with no custom behavior — this confirms
 * construction for both conversation-turn roles.
 */
class MentorMessageTest {

    @Test
    void accessors_ReturnConstructedValuesForUserRole() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        MentorMessage message = new MentorMessage(1L, 2L, "user", "Hello", createdAt);

        assertEquals(1L, message.id());
        assertEquals(2L, message.conversationId());
        assertEquals("user", message.role());
        assertEquals("Hello", message.text());
        assertEquals(createdAt, message.createdAt());
    }

    @Test
    void accessors_ReturnConstructedValuesForMentorRole() {
        MentorMessage message = new MentorMessage(1L, 2L, "mentor", "Hi there", Instant.now());

        assertEquals("mentor", message.role());
        assertEquals("Hi there", message.text());
    }
}
