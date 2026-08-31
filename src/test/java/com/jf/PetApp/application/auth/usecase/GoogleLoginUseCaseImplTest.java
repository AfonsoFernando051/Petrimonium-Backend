package com.jf.PetApp.application.auth.usecase;

import com.jf.PetApp.application.auth.dto.GoogleLoginCommand;
import com.jf.PetApp.application.auth.dto.LoginResult;
import com.jf.PetApp.application.auth.dto.RefreshTokenResult;
import com.jf.PetApp.application.auth.exception.AuthenticationException;
import com.jf.PetApp.application.auth.port.GoogleTokenVerifierPort;
import com.jf.PetApp.application.auth.port.GoogleUserInfo;
import com.jf.PetApp.application.auth.service.RefreshTokenIssuerService;
import com.jf.PetApp.application.gamification.service.StreakService;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AuthProviderEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class GoogleLoginUseCaseImplTest {

    @Mock
    private GoogleTokenVerifierPort googleTokenVerifier;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenIssuerService refreshTokenIssuerService;

    @Mock
    private StreakService streakService;

    @InjectMocks
    private GoogleLoginUseCaseImpl googleLoginUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_WithKnownProviderId_LogsInExistingGoogleUser() {
        GoogleUserInfo googleUser = new GoogleUserInfo("google-sub-1", "investor@test.com", "Investor");
        User user = new User();
        user.setId(1L);
        user.setProvider(AuthProviderEnum.GOOGLE);
        user.setProviderId("google-sub-1");

        when(googleTokenVerifier.verify("valid-token")).thenReturn(googleUser);
        when(userRepository.findByProviderId("google-sub-1")).thenReturn(Optional.of(user));
        when(refreshTokenIssuerService.issueFor(user)).thenReturn(new RefreshTokenResult("jwt-token", "refresh-token"));

        LoginResult result = googleLoginUseCase.execute(new GoogleLoginCommand("valid-token"));

        assertEquals("jwt-token", result.accessToken());
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_WithNewGoogleAccount_MatchingExistingLocalEmail_LinksTheAccount() {
        GoogleUserInfo googleUser = new GoogleUserInfo("google-sub-2", "investor@test.com", "Investor");
        User existingLocalUser = new User();
        existingLocalUser.setId(2L);
        existingLocalUser.setEmail("investor@test.com");
        existingLocalUser.setProvider(AuthProviderEnum.LOCAL);
        existingLocalUser.setPassword("hashed-password");

        when(googleTokenVerifier.verify("valid-token")).thenReturn(googleUser);
        when(userRepository.findByProviderId("google-sub-2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("investor@test.com")).thenReturn(Optional.of(existingLocalUser));
        when(userRepository.save(existingLocalUser)).thenReturn(existingLocalUser);
        when(refreshTokenIssuerService.issueFor(existingLocalUser)).thenReturn(new RefreshTokenResult("jwt-token", "refresh-token"));

        LoginResult result = googleLoginUseCase.execute(new GoogleLoginCommand("valid-token"));

        assertEquals("jwt-token", result.accessToken());
        assertEquals(AuthProviderEnum.GOOGLE, existingLocalUser.getProvider());
        assertEquals("google-sub-2", existingLocalUser.getProviderId());
        verify(userRepository).save(existingLocalUser);
    }

    @Test
    void execute_WithNoMatchingAccount_RegistersANewGoogleUser() {
        GoogleUserInfo googleUser = new GoogleUserInfo("google-sub-3", "newcomer@test.com", "Newcomer");

        when(googleTokenVerifier.verify("valid-token")).thenReturn(googleUser);
        when(userRepository.findByProviderId("google-sub-3")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newcomer@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenIssuerService.issueFor(any(User.class))).thenReturn(new RefreshTokenResult("jwt-token", "refresh-token"));

        LoginResult result = googleLoginUseCase.execute(new GoogleLoginCommand("valid-token"));

        assertEquals("jwt-token", result.accessToken());

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("newcomer@test.com", savedUser.getValue().getEmail());
        assertEquals("google-sub-3", savedUser.getValue().getProviderId());
        assertEquals(AuthProviderEnum.GOOGLE, savedUser.getValue().getProvider());
    }

    @Test
    void execute_WithInvalidToken_ShouldPropagateAuthenticationExceptionWithoutTouchingUserRepository() {
        when(googleTokenVerifier.verify("bad-token")).thenThrow(new AuthenticationException("Invalid Google token"));

        assertThrows(AuthenticationException.class, () ->
            googleLoginUseCase.execute(new GoogleLoginCommand("bad-token")));

        verifyNoInteractions(userRepository, refreshTokenIssuerService, streakService);
    }
}
