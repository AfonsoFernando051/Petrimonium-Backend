package com.jf.PetApp.infrastructure.config;

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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class AuthUseCaseConfigTest {

    private final AuthUseCaseConfig config = new AuthUseCaseConfig();

    @Test
    void refreshTokenIssuerService_WiresUpARefreshTokenIssuerService() {
        RefreshTokenIssuerService service = config.refreshTokenIssuerService(
                mock(RefreshTokenRepositoryPort.class), mock(TokenProvider.class), 30L);

        assertInstanceOf(RefreshTokenIssuerService.class, service);
    }

    @Test
    void loginUseCase_WiresUpALoginUseCaseImpl() {
        LoginUseCase useCase = config.loginUseCase(
                mock(UserRepository.class), mock(PasswordEncoderPort.class),
                mock(RefreshTokenIssuerService.class), mock(StreakService.class),
                mock(DemoAccountResetPort.class));

        assertInstanceOf(LoginUseCaseImpl.class, useCase);
    }

    @Test
    void googleLoginUseCase_WiresUpAGoogleLoginUseCaseImpl() {
        GoogleLoginUseCase useCase = config.googleLoginUseCase(
                mock(GoogleTokenVerifierPort.class), mock(UserRepository.class),
                mock(RefreshTokenIssuerService.class), mock(StreakService.class));

        assertInstanceOf(GoogleLoginUseCaseImpl.class, useCase);
    }

    @Test
    void registerUserUseCase_WiresUpARegisterUserUseCaseImpl() {
        RegisterUserUseCase useCase = config.registerUserUseCase(
                mock(UserRepository.class), mock(PasswordEncoderPort.class));

        assertInstanceOf(RegisterUserUseCaseImpl.class, useCase);
    }

    @Test
    void refreshTokenUseCase_WiresUpARefreshTokenUseCaseImpl() {
        RefreshTokenUseCase useCase = config.refreshTokenUseCase(
                mock(RefreshTokenRepositoryPort.class), mock(UserRepository.class),
                mock(RefreshTokenIssuerService.class));

        assertInstanceOf(RefreshTokenUseCaseImpl.class, useCase);
    }

    @Test
    void logoutUseCase_WiresUpALogoutUseCaseImpl() {
        LogoutUseCase useCase = config.logoutUseCase(mock(RefreshTokenRepositoryPort.class));

        assertInstanceOf(LogoutUseCaseImpl.class, useCase);
    }
}
