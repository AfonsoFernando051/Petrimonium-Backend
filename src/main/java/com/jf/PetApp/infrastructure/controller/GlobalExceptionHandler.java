package com.jf.PetApp.infrastructure.controller;

import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;

import com.jf.PetApp.application.auth.exception.AuthenticationException;
import com.jf.PetApp.application.auth.exception.PasswordResetTokenInvalidException;
import com.jf.PetApp.application.auth.exception.UserAlreadyExistsException;
import com.jf.PetApp.application.investment.exception.DestructivePortfolioReplaceException;
import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.health.exception.HealthConflictException;

/**
 * Single point where every uncaught exception becomes an HTTP response, so every controller
 * returns the same error shape: a {@link ProblemDetail} with a stable {@code code} and
 * {@code timestamp}. Client-caused errors (validation, "not found", "already exists", auth
 * failures) keep their specific status and a message that's safe to show a user. Anything else
 * — a bug, a database error, an unexpected null — is logged with its full stack trace here and
 * turned into a generic message; the caller never sees exception classes, SQL, stack traces, or
 * file paths.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_MESSAGE = "An unexpected error occurred. Please try again later.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (detail.isBlank()) {
            detail = "Please check the submitted fields.";
        }
        log.warn("Validation failed on {}: {}", e.getObjectName(), detail);
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException e) {
        String detail = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        if (detail.isBlank()) {
            detail = "Please check the submitted fields.";
        }
        log.warn("Validation failed: {}", detail);
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedBody(HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMostSpecificCause().getClass().getSimpleName());
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request body is malformed or contains invalid values.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Rejected request: {}", e.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
    }

    @ExceptionHandler(DestructivePortfolioReplaceException.class)
    public ProblemDetail handleDestructivePortfolioReplace(DestructivePortfolioReplaceException e) {
        // Logged at warn, not error: this is the guard doing its job against a
        // caller that didn't acknowledge the replacement — most likely an app
        // version predating the confirmReplace flag, not a server fault.
        log.warn("Rejected unconfirmed portfolio replacement: {} existing lot(s) -> {} submitted",
                e.currentLotCount(), e.submittedLotCount());
        return problem(HttpStatus.CONFLICT, "PORTFOLIO_REPLACE_NOT_CONFIRMED", e.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException e) {
        return problem(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", e.getMessage());
    }

    @ExceptionHandler(HealthConflictException.class)
    public ProblemDetail handleHealthConflict(HealthConflictException e) {
        log.warn("Health request conflict ({}): {}", e.code(), e.getMessage());
        return problem(HttpStatus.CONFLICT, e.code(), e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException e) {
        log.warn("Authentication failed");
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(PasswordResetTokenInvalidException.class)
    public ProblemDetail handlePasswordResetTokenInvalid(PasswordResetTokenInvalidException e) {
        log.warn("Rejected password reset: {}", e.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_TOKEN_INVALID", e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException e) {
        HttpStatusCode status = e.getStatusCode();
        if (status.is5xxServerError()) {
            log.error("Unexpected server error", e);
            return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", GENERIC_MESSAGE);
        }
        log.warn("Request rejected: {} {}", status, e.getReason());
        String detail = e.getReason() != null ? e.getReason() : "Request could not be processed.";
        return problem(HttpStatus.valueOf(status.value()), "REQUEST_ERROR", detail);
    }

    // Catch-all: anything not handled above is treated as a bug. Full details go to the
    // server log only — never to the client.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception e) {
        log.error("Unhandled exception", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", GENERIC_MESSAGE);
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setProperty("code", code);
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }
}
