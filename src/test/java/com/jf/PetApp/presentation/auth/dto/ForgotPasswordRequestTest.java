package com.jf.PetApp.presentation.auth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgotPasswordRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void accessorsReturnConstructedValues() {
        ForgotPasswordRequest dto = new ForgotPasswordRequest("investor@test.com");

        assertEquals("investor@test.com", dto.email());
    }

    @Test
    void validate_WithAValidEmail_HasNoViolations() {
        assertTrue(validator.validate(new ForgotPasswordRequest("investor@test.com")).isEmpty());
    }

    @Test
    void validate_WithBlankEmail_ProducesAViolation() {
        assertFalse(validator.validate(new ForgotPasswordRequest("")).isEmpty());
    }

    @Test
    void validate_WithMalformedEmail_ProducesAViolation() {
        assertFalse(validator.validate(new ForgotPasswordRequest("not-an-email")).isEmpty());
    }
}
