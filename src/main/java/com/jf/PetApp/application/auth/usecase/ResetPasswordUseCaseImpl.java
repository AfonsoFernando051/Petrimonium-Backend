package com.jf.PetApp.application.auth.usecase;

import java.time.Instant;

import com.jf.PetApp.application.auth.exception.PasswordResetTokenInvalidException;
import com.jf.PetApp.application.auth.port.PasswordEncoderPort;
import com.jf.PetApp.application.auth.port.PasswordResetTokenRepositoryPort;
import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.PasswordResetToken;
import com.jf.PetApp.core.domain.User;

public class ResetPasswordUseCaseImpl implements ResetPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepositoryPort tokenRepository;
    private final PasswordEncoderPort passwordEncoder;

    public ResetPasswordUseCaseImpl(
            UserRepository userRepository,
            PasswordResetTokenRepositoryPort tokenRepository,
            PasswordEncoderPort passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void execute(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(PasswordResetToken.hash(rawToken))
                .orElseThrow(PasswordResetTokenInvalidException::new);

        if (!token.isValid(Instant.now())) {
            throw new PasswordResetTokenInvalidException();
        }

        // The token was already validated as belonging to a real, persisted userId — the user
        // being gone at this point would mean the account was deleted after the token was
        // issued, a genuinely different failure than an invalid token.
        User user = userRepository.findById(token.userId().intValue())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.markUsed(token.id(), Instant.now());
    }
}
