package com.jf.PetApp.application.auth.usecase;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jf.PetApp.application.auth.dto.RefreshTokenCommand;
import com.jf.PetApp.application.auth.dto.RefreshTokenResult;
import com.jf.PetApp.application.auth.exception.AuthenticationException;
import com.jf.PetApp.application.auth.port.RefreshTokenRepositoryPort;
import com.jf.PetApp.application.auth.service.RefreshTokenIssuerService;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.RefreshToken;
import com.jf.PetApp.core.domain.User;

public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenUseCaseImpl.class);

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenIssuerService issuerService;

    public RefreshTokenUseCaseImpl(
            RefreshTokenRepositoryPort refreshTokenRepository,
            UserRepository userRepository,
            RefreshTokenIssuerService issuerService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.issuerService = issuerService;
    }

    @Override
    public RefreshTokenResult execute(RefreshTokenCommand command) throws AuthenticationException {
        String tokenHash = RefreshToken.hash(command.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthenticationException("Refresh token is invalid, expired, or revoked"));

        Instant now = Instant.now();

        if (stored.revokedAt() != null) {
            // A revoked token being presented again means either: the client double-submitted
            // a retry with a token that already got rotated (benign), or someone else has a
            // copy of a token the legitimate client already rotated past (theft). This can't
            // be told apart from the request alone, so the safe response is the same either
            // way: kill every active session for this user, forcing a real re-login.
            log.warn("Rejected reuse of a revoked refresh token for user {}", stored.userId());
            refreshTokenRepository.revokeAllForUser(stored.userId(), now);
            throw new AuthenticationException("Refresh token is invalid, expired, or revoked");
        }

        if (!stored.isValid(now)) {
            throw new AuthenticationException("Refresh token is invalid, expired, or revoked");
        }

        User user = userRepository.findById(stored.userId().intValue())
                .orElseThrow(() -> new AuthenticationException("Refresh token is invalid, expired, or revoked"));

        // The rotated token inherits the same app_context — a refresh can never switch which
        // app a session belongs to, only the original login/google login sets it.
        RefreshTokenResult result = issuerService.issueFor(user, stored.appContext());

        // Rotate: this token is now spent, and records which new one replaced it.
        refreshTokenRepository.revoke(stored.id(), now, RefreshToken.hash(result.refreshToken()));

        return result;
    }
}
