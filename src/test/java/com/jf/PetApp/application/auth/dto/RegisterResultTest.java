package com.jf.PetApp.application.auth.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegisterResultTest {

    @Test
    void accessorsReturnConstructedValues() {
        RegisterResult result = new RegisterResult(1L, "investor", "investor@test.com");

        assertEquals(1L, result.userId());
        assertEquals("investor", result.username());
        assertEquals("investor@test.com", result.email());
    }
}
