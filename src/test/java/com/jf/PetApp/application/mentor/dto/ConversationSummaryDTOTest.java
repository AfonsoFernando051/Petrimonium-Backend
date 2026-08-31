package com.jf.PetApp.application.mentor.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationSummaryDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        Instant updatedAt = Instant.parse("2026-08-23T00:00:00Z");

        ConversationSummaryDTO dto = new ConversationSummaryDTO(1L, "Title", updatedAt, "Last message");

        assertEquals(1L, dto.id());
        assertEquals("Title", dto.title());
        assertEquals(updatedAt, dto.updatedAt());
        assertEquals("Last message", dto.lastMessagePreview());
    }
}
