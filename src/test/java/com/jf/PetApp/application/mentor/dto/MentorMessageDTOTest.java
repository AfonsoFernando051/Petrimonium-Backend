package com.jf.PetApp.application.mentor.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MentorMessageDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        Instant createdAt = Instant.parse("2026-08-23T00:00:00Z");

        MentorMessageDTO dto = new MentorMessageDTO(1L, "mentor", "Hi there", createdAt);

        assertEquals(1L, dto.id());
        assertEquals("mentor", dto.role());
        assertEquals("Hi there", dto.text());
        assertEquals(createdAt, dto.createdAt());
    }
}
