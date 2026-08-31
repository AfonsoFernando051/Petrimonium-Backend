package com.jf.PetApp.infrastructure.security.password;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BCryptPasswordEncoderAdapterTest {

    private final BCryptPasswordEncoderAdapter adapter = new BCryptPasswordEncoderAdapter();

    @Test
    void encode_ProducesAHashDifferentFromTheRawPassword() {
        String encoded = adapter.encode("hunter2");

        assertNotEquals("hunter2", encoded);
    }

    @Test
    void encode_CalledTwiceOnTheSamePassword_ProducesDifferentHashes() {
        // BCrypt salts each hash, so two encodes of the same raw password must never match
        // verbatim even though both are valid.
        String first = adapter.encode("hunter2");
        String second = adapter.encode("hunter2");

        assertNotEquals(first, second);
    }

    @Test
    void matches_WithTheCorrectRawPassword_ReturnsTrue() {
        String encoded = adapter.encode("correct-horse-battery-staple");

        assertTrue(adapter.matches("correct-horse-battery-staple", encoded));
    }

    @Test
    void matches_WithTheWrongRawPassword_ReturnsFalse() {
        String encoded = adapter.encode("correct-horse-battery-staple");

        assertFalse(adapter.matches("wrong-password", encoded));
    }
}
