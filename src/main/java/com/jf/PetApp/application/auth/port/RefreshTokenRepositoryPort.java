package com.jf.PetApp.application.auth.port;

import java.time.Instant;
import java.util.Optional;

import com.jf.PetApp.core.domain.RefreshToken;

/**
 * Application-layer boundary for persisted refresh tokens. Use cases depend on this port,
 * never on Spring Data or JPA entities directly.
 */
public interface RefreshTokenRepositoryPort {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Marks this token revoked, recording which token replaced it (rotation) — pass null for a plain revoke (logout). */
    void revoke(Long tokenId, Instant revokedAt, String replacedByTokenHash);

    /** Theft-detection response: a revoked token was replayed, so the whole session family for this user is killed. */
    void revokeAllForUser(Long userId, Instant revokedAt);
}
