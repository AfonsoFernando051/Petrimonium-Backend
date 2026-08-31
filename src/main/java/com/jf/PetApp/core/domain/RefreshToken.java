package com.jf.PetApp.core.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * A long-lived, rotating credential that lets the mobile client silently obtain a new access
 * token without forcing the user to log in again every hour (the JWT access token's lifetime).
 * Only the SHA-256 hash ever reaches persistence ({@code tokenHash}) — same pattern as
 * {@link PasswordResetToken}. {@link #hash(String)} is the single place that turns a raw token
 * into what gets stored/looked-up.
 *
 * Rotation + reuse detection: each successful refresh revokes this row and issues a brand new
 * one ({@code replacedByTokenHash} records the chain). If a revoked token is ever presented
 * again, that's a strong signal it was stolen and the legitimate client already rotated past
 * it — see {@code RefreshTokenUseCaseImpl}, which responds by revoking every token for that
 * user, not just this one.
 */
public record RefreshToken(
        Long id,
        Long userId,
        String tokenHash,
        Instant expiresAt,
        Instant revokedAt,
        String replacedByTokenHash,
        Instant createdAt
) {

    public boolean isValid(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a JDK-mandatory algorithm (JCA standard names) — this is unreachable.
            throw new IllegalStateException(e);
        }
    }
}
