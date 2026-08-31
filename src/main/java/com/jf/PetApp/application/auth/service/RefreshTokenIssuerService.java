package com.jf.PetApp.application.auth.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import com.jf.PetApp.application.auth.dto.RefreshTokenResult;
import com.jf.PetApp.application.auth.port.RefreshTokenRepositoryPort;
import com.jf.PetApp.application.auth.port.TokenProvider;
import com.jf.PetApp.core.domain.RefreshToken;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;

/**
 * Issues a fresh access+refresh token pair for a user — the one place that logic lives, shared
 * by every login path (password, Google) and by {@code RefreshTokenUseCaseImpl}'s rotation
 * step, so a new issuance point (e.g. a future auth provider) can't accidentally diverge from
 * how the entropy/hashing/expiry is done everywhere else.
 */
public class RefreshTokenIssuerService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final TokenProvider tokenProvider;
    private final Duration refreshTokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenIssuerService(
            RefreshTokenRepositoryPort refreshTokenRepository,
            TokenProvider tokenProvider,
            Duration refreshTokenTtl
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    /** {@code appContext} may be null (no app_context requested/known for this session). */
    public RefreshTokenResult issueFor(User user, AppContextEnum appContext) {
        String accessToken = tokenProvider.generateToken(user, appContext);
        String rawRefreshToken = generateRawToken();

        Instant now = Instant.now();
        RefreshToken refreshToken = new RefreshToken(
                null,
                user.getId(),
                RefreshToken.hash(rawRefreshToken),
                now.plus(refreshTokenTtl),
                null,
                null,
                now,
                appContext
        );
        refreshTokenRepository.save(refreshToken);

        return new RefreshTokenResult(accessToken, rawRefreshToken);
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
