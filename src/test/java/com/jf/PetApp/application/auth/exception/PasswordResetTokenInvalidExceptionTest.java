package com.jf.PetApp.application.auth.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * HTTP-mapping behavior for this exception is already covered by
 * {@code GlobalExceptionHandlerTest#handlePasswordResetTokenInvalid_MapsToBadRequestWithSpecificCode}
 * -- this is just the standalone constructor/message test.
 */
class PasswordResetTokenInvalidExceptionTest {

    @Test
    void constructor_SetsTheFixedGenericMessage() {
        PasswordResetTokenInvalidException exception = new PasswordResetTokenInvalidException();

        assertEquals("Password reset token is invalid, expired, or already used", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }
}
