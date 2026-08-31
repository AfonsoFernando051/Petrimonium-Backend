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

class RenameConversationRequestTest {

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
    void accessorReturnsConstructedValue() {
        RenameConversationRequest dto = new RenameConversationRequest("New title");

        assertEquals("New title", dto.title());
    }

    @Test
    void validate_WithBlankTitle_ProducesAViolation() {
        RenameConversationRequest dto = new RenameConversationRequest(" ");

        Set<ConstraintViolation<RenameConversationRequest>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void validate_WithTitleOverSizeLimit_ProducesAViolation() {
        RenameConversationRequest dto = new RenameConversationRequest("a".repeat(256));

        Set<ConstraintViolation<RenameConversationRequest>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void validate_WithValidTitle_HasNoViolations() {
        RenameConversationRequest dto = new RenameConversationRequest("A perfectly fine title");

        assertTrue(validator.validate(dto).isEmpty());
    }
}
