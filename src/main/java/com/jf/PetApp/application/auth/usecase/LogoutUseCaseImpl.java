package com.jf.PetApp.application.auth.usecase;

import java.time.Instant;

import com.jf.PetApp.application.auth.dto.LogoutCommand;
import com.jf.PetApp.application.auth.port.RefreshTokenRepositoryPort;
import com.jf.PetApp.core.domain.RefreshToken;

/**
 * Revokes the refresh token server-side on logout, so a copy of it (already on disk somewhere,
 * a backup, a compromised device) can't silently keep minting new access tokens after the user
 * believes they've logged out. Idempotent by design — logging out with an already-invalid or
 * unknown token still "succeeds" from the client's perspective, matching how the access token
 * being cleared locally always succeeds regardless of server state.
 */
public class LogoutUseCaseImpl implements LogoutUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepository;

    public LogoutUseCaseImpl(RefreshTokenRepositoryPort refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void execute(LogoutCommand command) {
        if (command.refreshToken() == null || command.refreshToken().isBlank()) {
            return;
        }
        String tokenHash = RefreshToken.hash(command.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(token -> token.revokedAt() == null)
                .ifPresent(token -> refreshTokenRepository.revoke(token.id(), Instant.now(), null));
    }
}
