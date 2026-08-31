package com.jf.PetApp.application.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRequestTest {

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
        LoginRequest dto = new LoginRequest("investor@test.com", "hunter2");

        assertEquals("investor@test.com", dto.email());
        assertEquals("hunter2", dto.password());
    }

    @Test
    void validate_WithBlankEmail_ProducesAViolation() {
        LoginRequest dto = new LoginRequest("", "hunter2");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void validate_WithBlankPassword_ProducesAViolation() {
        LoginRequest dto = new LoginRequest("investor@test.com", "");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void validate_WithBothFieldsPresent_HasNoViolations() {
        LoginRequest dto = new LoginRequest("investor@test.com", "hunter2");

        assertTrue(validator.validate(dto).isEmpty());
    }
}
