package com.jf.PetApp.application.mentor.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MentorChatResponseTest {

    @Test
    void accessorsReturnConstructedValues() {
        MentorChatResponse dto = new MentorChatResponse(
                "Sure, let's talk about that.", 3L, "Retirement plan", List.of("portfolio_summary"));

        assertEquals("Sure, let's talk about that.", dto.reply());
        assertEquals(3L, dto.conversationId());
        assertEquals("Retirement plan", dto.title());
        assertEquals(List.of("portfolio_summary"), dto.sources());
    }
}
