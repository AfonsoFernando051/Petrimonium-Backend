package com.jf.PetApp.application.auth.usecase;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import com.jf.PetApp.application.auth.port.PasswordResetMailerPort;
import com.jf.PetApp.application.auth.port.PasswordResetTokenRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.PasswordResetToken;
import com.jf.PetApp.core.domain.User;

public class RequestPasswordResetUseCaseImpl implements RequestPasswordResetUseCase {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepositoryPort tokenRepository;
    private final PasswordResetMailerPort mailerPort;
    private final SecureRandom secureRandom = new SecureRandom();

    public RequestPasswordResetUseCaseImpl(
            UserRepository userRepository,
            PasswordResetTokenRepositoryPort tokenRepository,
            PasswordResetMailerPort mailerPort
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailerPort = mailerPort;
    }

    @Override
    public void execute(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // No account for this email: do nothing, silently. AuthController returns the
            // same generic response either way, so this early return is the only enforcement
            // point for "never reveal whether the account exists."
            return;
        }
        User user = userOpt.get();

        // Only the most recently requested token should ever be redeemable.
        tokenRepository.invalidateOutstandingForUser(user.getId());

        String rawToken = generateRawToken();
        Instant now = Instant.now();
        PasswordResetToken token = new PasswordResetToken(
                null, user.getId(), PasswordResetToken.hash(rawToken), now.plus(TOKEN_TTL), null, now);
        tokenRepository.save(token);

        mailerPort.sendPasswordResetEmail(user.getEmail(), rawToken);
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
