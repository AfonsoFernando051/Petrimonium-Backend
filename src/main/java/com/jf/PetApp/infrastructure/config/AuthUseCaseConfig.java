package com.jf.PetApp.infrastructure.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jf.PetApp.application.auth.port.GoogleTokenVerifierPort;
import com.jf.PetApp.application.auth.port.PasswordEncoderPort;
import com.jf.PetApp.application.auth.port.RefreshTokenRepositoryPort;
import com.jf.PetApp.application.auth.port.TokenProvider;
import com.jf.PetApp.application.auth.service.RefreshTokenIssuerService;
import com.jf.PetApp.application.auth.usecase.GoogleLoginUseCase;
import com.jf.PetApp.application.auth.usecase.GoogleLoginUseCaseImpl;
import com.jf.PetApp.application.auth.usecase.LoginUseCase;
import com.jf.PetApp.application.auth.usecase.LoginUseCaseImpl;
import com.jf.PetApp.application.auth.usecase.LogoutUseCase;
import com.jf.PetApp.application.auth.usecase.LogoutUseCaseImpl;
import com.jf.PetApp.application.auth.usecase.RefreshTokenUseCase;
import com.jf.PetApp.application.auth.usecase.RefreshTokenUseCaseImpl;
import com.jf.PetApp.application.auth.usecase.RegisterUserUseCase;
import com.jf.PetApp.application.auth.usecase.RegisterUserUseCaseImpl;
import com.jf.PetApp.application.gamification.service.StreakService;
import com.jf.PetApp.application.user.port.DemoAccountResetPort;
import com.jf.PetApp.application.user.port.UserRepository;

@Configuration
public class AuthUseCaseConfig {

    @Bean
    public RefreshTokenIssuerService refreshTokenIssuerService(
        RefreshTokenRepositoryPort refreshTokenRepository,
        TokenProvider tokenProvider,
        @Value("${app.refresh-token.expiration-days:30}") long refreshTokenExpirationDays
    ) {
        return new RefreshTokenIssuerService(
            refreshTokenRepository,
            tokenProvider,
            Duration.ofDays(refreshTokenExpirationDays)
        );
    }

    @Bean
    public LoginUseCase loginUseCase(
        UserRepository userRepository,
        PasswordEncoderPort passwordEncoder,
        RefreshTokenIssuerService refreshTokenIssuerService,
        StreakService streakService,
        DemoAccountResetPort demoAccountResetPort
    ) {
        return new LoginUseCaseImpl(
            userRepository,
            passwordEncoder,
            refreshTokenIssuerService,
            streakService,
            demoAccountResetPort
        );
    }

    @Bean
    public GoogleLoginUseCase googleLoginUseCase(
        GoogleTokenVerifierPort googleTokenVerifier,
        UserRepository userRepository,
        RefreshTokenIssuerService refreshTokenIssuerService,
        StreakService streakService
    ) {
        return new GoogleLoginUseCaseImpl(
            googleTokenVerifier,
            userRepository,
            refreshTokenIssuerService,
            streakService
        );
    }

    @Bean
    public RegisterUserUseCase registerUserUseCase(
        UserRepository userRepository,
        PasswordEncoderPort passwordEncoder
    ) {
        return new RegisterUserUseCaseImpl(
            userRepository,
            passwordEncoder
        );
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(
        RefreshTokenRepositoryPort refreshTokenRepository,
        UserRepository userRepository,
        RefreshTokenIssuerService refreshTokenIssuerService
    ) {
        return new RefreshTokenUseCaseImpl(refreshTokenRepository, userRepository, refreshTokenIssuerService);
    }

    @Bean
    public LogoutUseCase logoutUseCase(RefreshTokenRepositoryPort refreshTokenRepository) {
        return new LogoutUseCaseImpl(refreshTokenRepository);
    }
}
