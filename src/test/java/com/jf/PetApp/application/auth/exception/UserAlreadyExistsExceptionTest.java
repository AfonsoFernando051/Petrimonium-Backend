package com.jf.PetApp.application.auth.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * HTTP-mapping behavior for this exception is already covered by
 * {@code GlobalExceptionHandlerTest#handleUserAlreadyExists_MapsToConflictWithUserAlreadyExistsCode}
 * -- this is just the standalone constructor/message test.
 */
class UserAlreadyExistsExceptionTest {

    @Test
    void constructor_SetsTheFixedMessage() {
        UserAlreadyExistsException exception = new UserAlreadyExistsException();

        assertEquals("User already exists", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }
}
