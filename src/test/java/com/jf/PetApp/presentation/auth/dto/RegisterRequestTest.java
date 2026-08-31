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

class RegisterRequestTest {

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

    private static RegisterRequest valid() {
        return new RegisterRequest("investor01", "investor@test.com", "Hunter2Pass");
    }

    @Test
    void accessorsReturnConstructedValues() {
        RegisterRequest dto = valid();

        assertEquals("investor01", dto.username());
        assertEquals("investor@test.com", dto.email());
        assertEquals("Hunter2Pass", dto.password());
    }

    @Test
    void validate_WithAllFieldsValid_HasNoViolations() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    void validate_WithBlankUsername_ProducesAViolation() {
        assertFalse(validator.validate(new RegisterRequest("", "investor@test.com", "Hunter2Pass")).isEmpty());
    }

    @Test
    void validate_WithUsernameTooShort_ProducesAViolation() {
        assertFalse(validator.validate(new RegisterRequest("ab", "investor@test.com", "Hunter2Pass")).isEmpty());
    }

    @Test
    void validate_WithUsernameTooLong_ProducesAViolation() {
        String longUsername = "a".repeat(51);
        assertFalse(validator.validate(new RegisterRequest(longUsername, "investor@test.com", "Hunter2Pass")).isEmpty());
    }

    @Test
    void validate_WithMalformedEmail_ProducesAViolation() {
        assertFalse(validator.validate(new RegisterRequest("investor01", "not-an-email", "Hunter2Pass")).isEmpty());
    }

    @Test
    void validate_WithBlankPassword_ProducesAViolation() {
        assertFalse(validator.validate(new RegisterRequest("investor01", "investor@test.com", "")).isEmpty());
    }

    @Test
    void validate_WithPasswordTooShort_ProducesAViolation() {
        assertFalse(validator.validate(new RegisterRequest("investor01", "investor@test.com", "Ab1")).isEmpty());
    }

    @Test
    void validate_WithPasswordMissingAnUppercaseLetter_ProducesAViolation() {
        assertFalse(validator.validate(new RegisterRequest("investor01", "investor@test.com", "hunter2pass")).isEmpty());
    }

    @Test
    void validate_WithPasswordMissingALowercaseLetter_ProducesAViolation() {
        assertFalse(validator.validate(new RegisterRequest("investor01", "investor@test.com", "HUNTER2PASS")).isEmpty());
    }

    @Test
    void validate_WithPasswordMissingADigit_ProducesAViolation() {
        assertFalse(validator.validate(new RegisterRequest("investor01", "investor@test.com", "HunterPass")).isEmpty());
    }
}
