package com.jf.PetApp.application.auth.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegisterCommandTest {

    @Test
    void accessorsReturnConstructedValues() {
        RegisterCommand command = new RegisterCommand("investor01", "investor@test.com", "Hunter2!");

        assertEquals("investor01", command.username());
        assertEquals("investor@test.com", command.email());
        assertEquals("Hunter2!", command.password());
    }
}
