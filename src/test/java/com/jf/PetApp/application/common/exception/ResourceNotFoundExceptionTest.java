package com.jf.PetApp.application.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * HTTP-mapping behavior for this exception is already covered by
 * {@code GlobalExceptionHandlerTest#handleResourceNotFoundException_MapsToNotFoundWithResourceNotFoundCode}
 * -- this is just the standalone constructor/message test.
 */
class ResourceNotFoundExceptionTest {

    @Test
    void constructor_SetsTheGivenMessage() {
        ResourceNotFoundException exception = new ResourceNotFoundException("User not found");

        assertEquals("User not found", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
    }
}
