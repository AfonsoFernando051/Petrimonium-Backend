package com.jf.PetApp.application.auth.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginResponseTest {

    @Test
    void accessorReturnsConstructedValue() {
        LoginResponse dto = new LoginResponse("jwt-token-value", "refresh-token-value");

        assertEquals("jwt-token-value", dto.accessToken());
        assertEquals("refresh-token-value", dto.refreshToken());
    }
}
