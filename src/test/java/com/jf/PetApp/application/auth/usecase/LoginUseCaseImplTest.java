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
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
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
        when(refreshTokenIssuerService.issueFor(user, null)).thenReturn(new RefreshTokenResult("jwt-token", "refresh-token"));

        LoginResult result = loginUseCase.execute(new LoginCommand("investor@test.com", "correct-password"));

        assertEquals("jwt-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
    }

    @Test
    void execute_WithAppContextOnTheCommand_PassesItToTheIssuer() {
        User user = new User();
        user.setEmail("investor@test.com");
        user.setPassword("hashed-password");

        when(userRepository.findByEmail("investor@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(refreshTokenIssuerService.issueFor(user, AppContextEnum.WALLET))
                .thenReturn(new RefreshTokenResult("jwt-token", "refresh-token"));

        loginUseCase.execute(new LoginCommand("investor@test.com", "correct-password", AppContextEnum.WALLET));

        verify(refreshTokenIssuerService).issueFor(user, AppContextEnum.WALLET);
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

        verifyNoInteractions(refreshTokenIssuerService);
    }

    @Test
    void execute_WithUnknownEmail_ShouldThrowAuthenticationExceptionWithoutLeakingWhichFieldWasWrong() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("missing@test.com")).thenReturn(Optional.empty());

        // Same exception as a wrong password: the API must not reveal whether the
        // email or the password was the invalid part of the credentials pair.
        assertThrows(AuthenticationException.class, () ->
            loginUseCase.execute(new LoginCommand("missing@test.com", "any-password")));

        verifyNoInteractions(refreshTokenIssuerService);
    }

    /**
     * The error message alone is not the whole answer surface. Returning before any hashing
     * happens makes an unknown account answer measurably faster than a known one with a wrong
     * password, which is a working account-enumeration oracle — the same thing
     * {@code RequestPasswordResetUseCaseImpl} deliberately refuses to leak on the
     * forgot-password path. So the miss path pays the same BCrypt cost as a real verification.
     */
    @Test
    void execute_WithUnknownEmail_StillVerifiesAPasswordSoTheMissCostsTheSameAsAHit() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () ->
            loginUseCase.execute(new LoginCommand("missing@test.com", "any-password")));

        verify(passwordEncoder).matches("any-password", LoginUseCaseImpl.NO_SUCH_USER_PASSWORD_HASH);
    }

    /**
     * A Google-created account has {@code password == null} (see {@code User.createFromGoogle}).
     * Such an account must never be reachable by the password path, and the null must never
     * reach the encoder — both of which the dummy-hash substitution handles in one place.
     */
    @Test
    void execute_ForAGoogleAccountWithNoLocalPassword_NeverPassesNullToTheEncoder() {
        User googleUser = new User();
        googleUser.setEmail("investor@test.com");
        googleUser.setPassword(null);

        when(userRepository.findByEmail("investor@test.com")).thenReturn(Optional.of(googleUser));

        assertThrows(AuthenticationException.class, () ->
            loginUseCase.execute(new LoginCommand("investor@test.com", "any-password")));

        verify(passwordEncoder).matches("any-password", LoginUseCaseImpl.NO_SUCH_USER_PASSWORD_HASH);
        verifyNoInteractions(refreshTokenIssuerService);
    }

    /**
     * Guards the one way this could go wrong silently: if the dummy hash were ever replaced by
     * something a caller can guess the plaintext of, {@code matches} would return true on the
     * miss path and the "user was not found" branch would be the only thing left standing
     * between an attacker and a token.
     */
    @Test
    void execute_WithUnknownEmail_RefusesEvenIfTheDummyHashSomehowMatches() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("missing@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThrows(AuthenticationException.class, () ->
            loginUseCase.execute(new LoginCommand("missing@test.com", "any-password")));

        verifyNoInteractions(refreshTokenIssuerService);
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
        when(refreshTokenIssuerService.issueFor(user, null)).thenReturn(new RefreshTokenResult("jwt-token", "refresh-token"));

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
        when(refreshTokenIssuerService.issueFor(user, null)).thenReturn(new RefreshTokenResult("jwt-token", "refresh-token"));

        loginUseCase.execute(new LoginCommand("admin2@petinvest.local", "correct-password"));

        verify(demoAccountResetPort).resetIfDemoAccount("admin2");
    }
}
