package com.jf.PetApp.application.mentor.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MentorChatResponseTest {

    @Test
    void accessorsReturnConstructedValues() {
        MentorChatResponse dto = new MentorChatResponse("Sure, let's talk about that.", 3L, "Retirement plan");

        assertEquals("Sure, let's talk about that.", dto.reply());
        assertEquals(3L, dto.conversationId());
        assertEquals("Retirement plan", dto.title());
    }
}
