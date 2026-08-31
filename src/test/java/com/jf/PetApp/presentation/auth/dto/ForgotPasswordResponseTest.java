package com.jf.PetApp.presentation.auth.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForgotPasswordResponseTest {

    @Test
    void accessorsReturnConstructedValues() {
        ForgotPasswordResponse dto = new ForgotPasswordResponse("If that email exists, a reset link was sent.");

        assertEquals("If that email exists, a reset link was sent.", dto.message());
    }
}
