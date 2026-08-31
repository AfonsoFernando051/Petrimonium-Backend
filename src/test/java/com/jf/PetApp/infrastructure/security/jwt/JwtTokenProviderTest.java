package com.jf.PetApp.infrastructure.security.jwt;

import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.RoleEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    // Test-only signing key — not a real secret, never used outside this test.
    private static final String TEST_SECRET = "test-only-jwt-signing-key-never-used-outside-tests-0123456789";

    private User userWith(String email, RoleEnum role) {
        User user = new User();
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    @Test
    void generateToken_ThenExtractSubject_RoundTripsTheUsersEmail() {
        JwtTokenProvider provider = new JwtTokenProvider(TEST_SECRET, 3_600_000);
        User user = userWith("investor@test.com", RoleEnum.USER);

        String token = provider.generateToken(user);

        assertEquals("investor@test.com", provider.extractSubject(token));
    }

    @Test
    void generateToken_EmbedsTheUsersRoleAsAClaim() {
        JwtTokenProvider provider = new JwtTokenProvider(TEST_SECRET, 3_600_000);
        User user = userWith("admin@test.com", RoleEnum.ADMIN);

        String token = provider.generateToken(user);

        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        assertEquals("ADMIN", claims.get("role"));
    }

    @Test
    void validate_WithATokenItSignedItself_ReturnsTrue() {
        JwtTokenProvider provider = new JwtTokenProvider(TEST_SECRET, 3_600_000);
        String token = provider.generateToken(userWith("investor@test.com", RoleEnum.USER));

        assertTrue(provider.validate(token));
    }

    @Test
    void validate_WithGarbageInput_ReturnsFalseRatherThanThrowing() {
        JwtTokenProvider provider = new JwtTokenProvider(TEST_SECRET, 3_600_000);

        assertFalse(provider.validate("not-a-real-token"));
    }

    @Test
    void validate_WithEmptyString_ReturnsFalse() {
        JwtTokenProvider provider = new JwtTokenProvider(TEST_SECRET, 3_600_000);

        assertFalse(provider.validate(""));
    }

    @Test
    void validate_WithAnAlreadyExpiredToken_ReturnsFalse() {
        // Negative expiration means the token's expiry is already in the past
        // the instant it's minted.
        JwtTokenProvider provider = new JwtTokenProvider(TEST_SECRET, -1000);
        String token = provider.generateToken(userWith("investor@test.com", RoleEnum.USER));

        assertFalse(provider.validate(token));
    }

    @Test
    void validate_WithATokenSignedByADifferentKey_ReturnsFalse() {
        JwtTokenProvider providerA = new JwtTokenProvider(TEST_SECRET, 3_600_000);
        JwtTokenProvider providerB = new JwtTokenProvider("a-completely-different-test-signing-key-0123456789ABCDEF", 3_600_000);

        String tokenFromA = providerA.generateToken(userWith("investor@test.com", RoleEnum.USER));

        assertFalse(providerB.validate(tokenFromA));
    }

    @Test
    void constructor_RejectsASecretShorterThan256Bits() {
        assertThrows(Exception.class, () -> new JwtTokenProvider("too-short", 3_600_000));
    }
}
