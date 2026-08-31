package com.jf.PetApp.application.mentor.dto;

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

class MentorClientContextDTOTest {

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
        MentorClientContextDTO dto = new MentorClientContextDTO("Retire early", "long-term", "chat", "en");

        assertEquals("Retire early", dto.petGoal());
        assertEquals("long-term", dto.investmentHorizon());
        assertEquals("chat", dto.currentScreen());
        assertEquals("en", dto.language());
    }

    @Test
    void validate_WithAllFieldsWithinBounds_HasNoViolations() {
        MentorClientContextDTO dto = new MentorClientContextDTO("Retire early", "long-term", "chat", "en");

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void validate_WithNullFields_HasNoViolations() {
        MentorClientContextDTO dto = new MentorClientContextDTO(null, null, null, null);

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void validate_WithPetGoalOverLimit_ProducesAViolation() {
        MentorClientContextDTO dto = new MentorClientContextDTO("g".repeat(201), null, null, null);

        Set<ConstraintViolation<MentorClientContextDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void validate_WithLanguageOverLimit_ProducesAViolation() {
        MentorClientContextDTO dto = new MentorClientContextDTO(null, null, null, "a".repeat(11));

        Set<ConstraintViolation<MentorClientContextDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }
}
