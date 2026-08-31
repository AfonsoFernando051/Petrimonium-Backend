package com.jf.PetApp.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class PasswordResetTokenTest {

    @Test
    void isValid_NotUsedAndBeforeExpiry_ReturnsTrue() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        PasswordResetToken token = new PasswordResetToken(
                1L, 2L, "hash", now.plusSeconds(60), null, now.minusSeconds(60));

        assertTrue(token.isValid(now));
    }

    @Test
    void isValid_AlreadyUsed_ReturnsFalse() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        PasswordResetToken token = new PasswordResetToken(
                1L, 2L, "hash", now.plusSeconds(60), now.minusSeconds(10), now.minusSeconds(60));

        assertFalse(token.isValid(now));
    }

    @Test
    void isValid_PastExpiry_ReturnsFalse() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        PasswordResetToken token = new PasswordResetToken(
                1L, 2L, "hash", now.minusSeconds(1), null, now.minusSeconds(60));

        assertFalse(token.isValid(now));
    }

    @Test
    void isValid_ExactlyAtExpiry_ReturnsFalse() {
        Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");
        PasswordResetToken token = new PasswordResetToken(
                1L, 2L, "hash", expiresAt, null, expiresAt.minusSeconds(60));

        // now.isBefore(expiresAt) is false when now == expiresAt.
        assertFalse(token.isValid(expiresAt));
    }

    @Test
    void hash_SameInput_ProducesSameHash() {
        assertEquals(PasswordResetToken.hash("raw-token"), PasswordResetToken.hash("raw-token"));
    }

    @Test
    void hash_DifferentInput_ProducesDifferentHash() {
        assertNotEquals(PasswordResetToken.hash("raw-token-a"), PasswordResetToken.hash("raw-token-b"));
    }

    @Test
    void hash_ProducesA64CharacterHexString() {
        String hash = PasswordResetToken.hash("raw-token");

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }
}
