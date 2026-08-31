package com.jf.PetApp.application.auth.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginCommandTest {

    @Test
    void accessorsReturnConstructedValues() {
        LoginCommand dto = new LoginCommand("investor@test.com", "hunter2");

        assertEquals("investor@test.com", dto.email());
        assertEquals("hunter2", dto.password());
    }
}
