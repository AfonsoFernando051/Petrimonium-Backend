package com.jf.PetApp.presentation.auth.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegisterResponseTest {

    @Test
    void accessorsReturnConstructedValues() {
        RegisterResponse dto = new RegisterResponse(1L, "investor", "investor@test.com");

        assertEquals(1L, dto.userId());
        assertEquals("investor", dto.username());
        assertEquals("investor@test.com", dto.email());
    }
}
