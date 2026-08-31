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

class MentorChatRequestTest {

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
        MentorClientContextDTO context = new MentorClientContextDTO("Retire early", "long-term", "chat", "en");
        MentorChatRequest dto = new MentorChatRequest("Hello mentor", 5L, context);

        assertEquals("Hello mentor", dto.message());
        assertEquals(5L, dto.conversationId());
        assertEquals(context, dto.context());
    }

    @Test
    void validate_WithBlankMessage_ProducesAViolation() {
        MentorChatRequest dto = new MentorChatRequest("", null, null);

        Set<ConstraintViolation<MentorChatRequest>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void validate_WithMessageOverSizeLimit_ProducesAViolation() {
        String tooLong = "a".repeat(2001);
        MentorChatRequest dto = new MentorChatRequest(tooLong, null, null);

        Set<ConstraintViolation<MentorChatRequest>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void validate_WithValidMessageAndNoContext_HasNoViolations() {
        MentorChatRequest dto = new MentorChatRequest("Hello mentor", null, null);

        Set<ConstraintViolation<MentorChatRequest>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validate_CascadesIntoAnInvalidNestedContext() {
        MentorClientContextDTO invalidContext = new MentorClientContextDTO("g".repeat(201), null, null, null);
        MentorChatRequest dto = new MentorChatRequest("Hello mentor", null, invalidContext);

        Set<ConstraintViolation<MentorChatRequest>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }
}
