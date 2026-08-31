package com.jf.PetApp.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/**
 * MentorConversation is a plain record with no custom behavior — this
 * confirms construction and the documented "null title until first message"
 * convention.
 */
class MentorConversationTest {

    @Test
    void accessors_ReturnConstructedValues() {
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Instant updated = Instant.parse("2026-01-02T00:00:00Z");
        MentorConversation conversation = new MentorConversation(1L, "user@test.com", "My Chat", created, updated, "wallet");

        assertEquals(1L, conversation.id());
        assertEquals("user@test.com", conversation.userEmail());
        assertEquals("My Chat", conversation.title());
        assertEquals(created, conversation.createdAt());
        assertEquals(updated, conversation.updatedAt());
        assertEquals("wallet", conversation.appContext());
    }

    @Test
    void title_CanBeNullBeforeFirstMessage() {
        Instant now = Instant.now();
        MentorConversation conversation = new MentorConversation(1L, "user@test.com", null, now, now, "academy");

        assertNull(conversation.title());
    }

    @Test
    void appContext_CanBeNullForAConversationCreatedBeforeTheClaimExisted() {
        Instant now = Instant.now();
        MentorConversation conversation = new MentorConversation(1L, "user@test.com", "Old chat", now, now, null);

        assertNull(conversation.appContext());
    }
}
