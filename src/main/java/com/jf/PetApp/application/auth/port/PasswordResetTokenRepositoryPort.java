package com.jf.PetApp.application.auth.port;

import java.time.Instant;
import java.util.Optional;

import com.jf.PetApp.core.domain.PasswordResetToken;

/**
 * Application-layer boundary for persisted password-reset tokens. Use cases depend on this
 * port, never on Spring Data or JPA entities directly.
 */
public interface PasswordResetTokenRepositoryPort {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Marks every not-yet-used token for this user as used, so only the newest request stays redeemable. */
    void invalidateOutstandingForUser(Long userId);

    void markUsed(Long tokenId, Instant usedAt);
}
