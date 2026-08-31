package com.jf.PetApp.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jf.PetApp.application.auth.port.PasswordEncoderPort;
import com.jf.PetApp.application.auth.port.PasswordResetMailerPort;
import com.jf.PetApp.application.auth.port.PasswordResetTokenRepositoryPort;
import com.jf.PetApp.application.auth.usecase.RequestPasswordResetUseCase;
import com.jf.PetApp.application.auth.usecase.RequestPasswordResetUseCaseImpl;
import com.jf.PetApp.application.auth.usecase.ResetPasswordUseCase;
import com.jf.PetApp.application.auth.usecase.ResetPasswordUseCaseImpl;
import com.jf.PetApp.application.user.port.UserRepository;

@Configuration
public class PasswordResetUseCaseConfig {

    @Bean
    public RequestPasswordResetUseCase requestPasswordResetUseCase(
        UserRepository userRepository,
        PasswordResetTokenRepositoryPort tokenRepository,
        PasswordResetMailerPort mailerPort
    ) {
        return new RequestPasswordResetUseCaseImpl(userRepository, tokenRepository, mailerPort);
    }

    @Bean
    public ResetPasswordUseCase resetPasswordUseCase(
        UserRepository userRepository,
        PasswordResetTokenRepositoryPort tokenRepository,
        PasswordEncoderPort passwordEncoder
    ) {
        return new ResetPasswordUseCaseImpl(userRepository, tokenRepository, passwordEncoder);
    }
}
