package com.jf.PetApp.application.mentor.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MentorTurnDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        MentorTurnDTO dto = new MentorTurnDTO("user", "What should I invest in?");

        assertEquals("user", dto.role());
        assertEquals("What should I invest in?", dto.text());
    }
}
