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

class ResetPasswordRequestTest {

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

    private static ResetPasswordRequest valid() {
        return new ResetPasswordRequest("raw-token-value", "Hunter2Pass");
    }

    @Test
    void accessorsReturnConstructedValues() {
        ResetPasswordRequest dto = valid();

        assertEquals("raw-token-value", dto.token());
        assertEquals("Hunter2Pass", dto.newPassword());
    }

    @Test
    void validate_WithAllFieldsValid_HasNoViolations() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    void validate_WithBlankToken_ProducesAViolation() {
        assertFalse(validator.validate(new ResetPasswordRequest("", "Hunter2Pass")).isEmpty());
    }

    @Test
    void validate_WithBlankNewPassword_ProducesAViolation() {
        assertFalse(validator.validate(new ResetPasswordRequest("raw-token-value", "")).isEmpty());
    }

    @Test
    void validate_WithNewPasswordTooShort_ProducesAViolation() {
        assertFalse(validator.validate(new ResetPasswordRequest("raw-token-value", "Ab1")).isEmpty());
    }

    @Test
    void validate_WithNewPasswordMissingAnUppercaseLetter_ProducesAViolation() {
        assertFalse(validator.validate(new ResetPasswordRequest("raw-token-value", "hunter2pass")).isEmpty());
    }

    @Test
    void validate_WithNewPasswordMissingADigit_ProducesAViolation() {
        assertFalse(validator.validate(new ResetPasswordRequest("raw-token-value", "HunterPass")).isEmpty());
    }
}
