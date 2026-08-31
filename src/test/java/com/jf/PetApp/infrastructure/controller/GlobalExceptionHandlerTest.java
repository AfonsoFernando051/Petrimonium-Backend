package com.jf.PetApp.infrastructure.controller;

import com.jf.PetApp.application.auth.exception.AuthenticationException;
import com.jf.PetApp.application.auth.exception.PasswordResetTokenInvalidException;
import com.jf.PetApp.application.auth.exception.UserAlreadyExistsException;
import com.jf.PetApp.application.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * One test per exception type this handler maps — mirrors the priority-item-7 intent from the
 * remediation plan: this class alone would have caught the pre-fix bug where "not found" use
 * cases threw {@link IllegalArgumentException} (mapped to 400) instead of
 * {@link ResourceNotFoundException} (mapped to 404).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidation_MapsToBadRequestWithValidationErrorCode() {
        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.rejectValue(null, "error", "field is invalid");
        when(e.getBindingResult()).thenReturn(bindingResult);
        when(e.getObjectName()).thenReturn("request");

        ProblemDetail result = handler.handleValidation(e);

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals("VALIDATION_ERROR", result.getProperties().get("code"));
    }

    @Test
    void handleConstraintViolation_MapsToBadRequestWithValidationErrorCode() {
        ConstraintViolationException e = new ConstraintViolationException(Set.of());

        ProblemDetail result = handler.handleConstraintViolation(e);

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals("VALIDATION_ERROR", result.getProperties().get("code"));
    }

    @Test
    void handleMalformedBody_MapsToBadRequestWithMalformedRequestCode() {
        HttpMessageNotReadableException e = mock(HttpMessageNotReadableException.class);
        when(e.getMostSpecificCause()).thenReturn(new RuntimeException("bad json"));

        ProblemDetail result = handler.handleMalformedBody(e);

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals("MALFORMED_REQUEST", result.getProperties().get("code"));
    }

    @Test
    void handleIllegalArgumentException_MapsToBadRequestWithInvalidRequestCode() {
        ProblemDetail result = handler.handleIllegalArgumentException(new IllegalArgumentException("bad input"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals("INVALID_REQUEST", result.getProperties().get("code"));
        assertEquals("bad input", result.getDetail());
    }

    @Test
    void handleUserAlreadyExists_MapsToConflictWithUserAlreadyExistsCode() {
        ProblemDetail result = handler.handleUserAlreadyExists(new UserAlreadyExistsException());

        assertEquals(HttpStatus.CONFLICT.value(), result.getStatus());
        assertEquals("USER_ALREADY_EXISTS", result.getProperties().get("code"));
    }

    @Test
    void handleAuthenticationException_MapsToUnauthorizedWithInvalidCredentialsCode() {
        ProblemDetail result = handler.handleAuthenticationException(new AuthenticationException());

        assertEquals(HttpStatus.UNAUTHORIZED.value(), result.getStatus());
        assertEquals("INVALID_CREDENTIALS", result.getProperties().get("code"));
    }

    @Test
    void handleResourceNotFoundException_MapsToNotFoundWithResourceNotFoundCode() {
        ProblemDetail result = handler.handleResourceNotFoundException(new ResourceNotFoundException("User not found"));

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals("RESOURCE_NOT_FOUND", result.getProperties().get("code"));
        assertEquals("User not found", result.getDetail());
    }

    @Test
    void handlePasswordResetTokenInvalid_MapsToBadRequestWithSpecificCode() {
        ProblemDetail result = handler.handlePasswordResetTokenInvalid(new PasswordResetTokenInvalidException());

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals("PASSWORD_RESET_TOKEN_INVALID", result.getProperties().get("code"));
    }

    @Test
    void handleResponseStatusException_With4xx_PreservesTheOriginalStatus() {
        ResponseStatusException e = new ResponseStatusException(HttpStatus.NOT_FOUND, "not here");

        ProblemDetail result = handler.handleResponseStatusException(e);

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals("REQUEST_ERROR", result.getProperties().get("code"));
        assertEquals("not here", result.getDetail());
    }

    @Test
    void handleResponseStatusException_With5xx_MasksTheRealStatusAsGenericInternalError() {
        ResponseStatusException e = new ResponseStatusException(HttpStatus.BAD_GATEWAY, "upstream down");

        ProblemDetail result = handler.handleResponseStatusException(e);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getStatus());
        assertEquals("INTERNAL_ERROR", result.getProperties().get("code"));
        // The real upstream reason must never leak to the client for a 5xx.
        assertNotNull(result.getDetail());
    }

    @Test
    void handleException_CatchAll_MapsToGenericInternalErrorWithoutLeakingDetails() {
        ProblemDetail result = handler.handleException(new RuntimeException("some internal secret detail"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getStatus());
        assertEquals("INTERNAL_ERROR", result.getProperties().get("code"));
        assertEquals("An unexpected error occurred. Please try again later.", result.getDetail());
    }

    @Test
    void everyMappedProblemDetail_IncludesATimestamp() {
        ProblemDetail result = handler.handleUserAlreadyExists(new UserAlreadyExistsException());

        assertNotNull(result.getProperties().get("timestamp"));
    }
}
