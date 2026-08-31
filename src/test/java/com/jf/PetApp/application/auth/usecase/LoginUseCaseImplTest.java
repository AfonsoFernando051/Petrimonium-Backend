package com.jf.PetApp.application.auth.usecase;

import com.jf.PetApp.application.auth.dto.LoginCommand;
import com.jf.PetApp.application.auth.dto.LoginResult;
import com.jf.PetApp.application.auth.dto.RefreshTokenResult;
import com.jf.PetApp.application.auth.exception.AuthenticationException;
import com.jf.PetApp.application.auth.port.PasswordEncoderPort;
import com.jf.PetApp.application.auth.service.RefreshTokenIssuerService;
import com.jf.PetApp.application.gamification.service.StreakService;
import com.jf.PetApp.application.user.port.DemoAccountResetPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LoginUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private RefreshTokenIssuerService refreshTokenIssuerService;

    @Mock
    private StreakService streakService;

    @Mock
    private DemoAccountResetPort demoAccountResetPort;

    @InjectMocks
    private LoginUseCaseImpl loginUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_WithValidCredentials_ShouldReturnAccessToken() {
        User user = new User();
        user.setEmail("investor@test.com");
        user.setPassword("hashed-password");

        when(userRepository.findByEmail("investor@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(refreshTokenIssuerService.issueFor(user)).thenReturn(new RefreshTokenResult("jwt-token", "refresh-token"));

        LoginResult result = loginUseCase.execute(new LoginCommand("investor@test.com", "correct-password"));

        assertEquals("jwt-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
    }

    @Test
    void execute_WithWrongPassword_ShouldThrowAuthenticationExceptionWithoutIssuingToken() {
        User user = new User();
        user.setEmail("investor@test.com");
        user.setPassword("hashed-password");

        when(userRepository.findByEmail("investor@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(AuthenticationException.class, () ->
            loginUseCase.execute(new LoginCommand("investor@test.com", "wrong-password")));

        verify(refreshTokenIssuerService, never()).issueFor(any());
    }

    @Test
    void execute_WithUnknownEmail_ShouldThrowAuthenticationExceptionWithoutLeakingWhichFieldWasWrong() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("missing@test.com")).thenReturn(Optional.empty());

        // Same exception as a wrong password: the API must not reveal whether the
        // email or the password was the invalid part of the credentials pair.
        assertThrows(AuthenticationException.class, () ->
            loginUseCase.execute(new LoginCommand("missing@test.com", "any-password")));

        verifyNoInteractions(passwordEncoder, refreshTokenIssuerService);
    }

    @Test
    void execute_WithUsernameInsteadOfEmail_ShouldReturnAccessToken() {
        User user = new User();
        user.setUsername("investor");
        user.setEmail("investor@test.com");
        user.setPassword("hashed-password");

        when(userRepository.findByEmail("investor")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("investor")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(refreshTokenIssuerService.issueFor(user)).thenReturn(new RefreshTokenResult("jwt-token", "refresh-token"));

        LoginResult result = loginUseCase.execute(new LoginCommand("investor", "correct-password"));

        assertEquals("jwt-token", result.accessToken());
    }

    @Test
    void execute_WithValidCredentials_InvokesDemoAccountResetForTheLoggedInUsername() {
        User user = new User();
        user.setUsername("admin2");
        user.setEmail("admin2@petinvest.local");
        user.setPassword("hashed-password");

        when(userRepository.findByEmail("admin2@petinvest.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(refreshTokenIssuerService.issueFor(user)).thenReturn(new RefreshTokenResult("jwt-token", "refresh-token"));

        loginUseCase.execute(new LoginCommand("admin2@petinvest.local", "correct-password"));

        verify(demoAccountResetPort).resetIfDemoAccount("admin2");
    }
}
