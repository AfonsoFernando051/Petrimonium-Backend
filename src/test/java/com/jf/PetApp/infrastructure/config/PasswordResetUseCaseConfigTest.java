package com.jf.PetApp.infrastructure.config;

import com.jf.PetApp.application.auth.port.PasswordEncoderPort;
import com.jf.PetApp.application.auth.port.PasswordResetMailerPort;
import com.jf.PetApp.application.auth.port.PasswordResetTokenRepositoryPort;
import com.jf.PetApp.application.auth.usecase.RequestPasswordResetUseCase;
import com.jf.PetApp.application.auth.usecase.RequestPasswordResetUseCaseImpl;
import com.jf.PetApp.application.auth.usecase.ResetPasswordUseCase;
import com.jf.PetApp.application.auth.usecase.ResetPasswordUseCaseImpl;
import com.jf.PetApp.application.user.port.UserRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class PasswordResetUseCaseConfigTest {

    private final PasswordResetUseCaseConfig config = new PasswordResetUseCaseConfig();

    @Test
    void requestPasswordResetUseCase_WiresUpARequestPasswordResetUseCaseImpl() {
        RequestPasswordResetUseCase useCase = config.requestPasswordResetUseCase(
                mock(UserRepository.class), mock(PasswordResetTokenRepositoryPort.class),
                mock(PasswordResetMailerPort.class));

        assertInstanceOf(RequestPasswordResetUseCaseImpl.class, useCase);
    }

    @Test
    void resetPasswordUseCase_WiresUpAResetPasswordUseCaseImpl() {
        ResetPasswordUseCase useCase = config.resetPasswordUseCase(
                mock(UserRepository.class), mock(PasswordResetTokenRepositoryPort.class),
                mock(PasswordEncoderPort.class));

        assertInstanceOf(ResetPasswordUseCaseImpl.class, useCase);
    }
}
