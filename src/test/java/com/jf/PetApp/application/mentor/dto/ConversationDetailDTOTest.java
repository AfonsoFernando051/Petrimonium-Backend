package com.jf.PetApp.application.mentor.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationDetailDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        MentorMessageDTO message = new MentorMessageDTO(1L, "user", "Hi", Instant.EPOCH);

        ConversationDetailDTO dto = new ConversationDetailDTO(1L, "My conversation", List.of(message));

        assertEquals(1L, dto.id());
        assertEquals("My conversation", dto.title());
        assertEquals(List.of(message), dto.messages());
    }
}
