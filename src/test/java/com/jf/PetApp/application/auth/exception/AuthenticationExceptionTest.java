package com.jf.PetApp.application.auth.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * HTTP-mapping behavior for this exception is already covered by
 * {@code GlobalExceptionHandlerTest#handleAuthenticationException_MapsToUnauthorizedWithInvalidCredentialsCode}
 * -- this is just the standalone constructor/message test.
 */
class AuthenticationExceptionTest {

    @Test
    void constructor_SetsTheFixedMessage() {
        AuthenticationException exception = new AuthenticationException();

        assertEquals("Invalid email or password", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }
}
