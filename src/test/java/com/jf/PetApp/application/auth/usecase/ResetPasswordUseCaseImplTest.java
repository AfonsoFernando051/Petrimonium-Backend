package com.jf.PetApp.application.auth.usecase;

import com.jf.PetApp.application.auth.exception.PasswordResetTokenInvalidException;
import com.jf.PetApp.application.auth.port.PasswordEncoderPort;
import com.jf.PetApp.application.auth.port.PasswordResetTokenRepositoryPort;
import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.PasswordResetToken;
import com.jf.PetApp.core.domain.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResetPasswordUseCaseImplTest {

    private static final String RAW_TOKEN = "raw-token-value";
    private static final String TOKEN_HASH = PasswordResetToken.hash(RAW_TOKEN);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepositoryPort tokenRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    private ResetPasswordUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new ResetPasswordUseCaseImpl(userRepository, tokenRepository, passwordEncoder);
    }

    private PasswordResetToken validToken() {
        return new PasswordResetToken(10L, 1L, TOKEN_HASH, Instant.now().plusSeconds(1800), null, Instant.now());
    }

    private User userWith(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    @Test
    void execute_WithValidToken_UpdatesThePasswordAndMarksTheTokenUsed() {
        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(validToken()));
        when(userRepository.findById(1)).thenReturn(Optional.of(userWith(1L, "investor@test.com")));
        when(passwordEncoder.encode("NewStr0ngPass")).thenReturn("hashed-new-password");

        useCase.execute(RAW_TOKEN, "NewStr0ngPass");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("hashed-new-password", userCaptor.getValue().getPassword());
        verify(tokenRepository).markUsed(eq(10L), org.mockito.ArgumentMatchers.any(Instant.class));
    }

    @Test
    void execute_WithUnknownToken_ThrowsPasswordResetTokenInvalidExceptionWithoutTouchingTheUser() {
        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        assertThrows(PasswordResetTokenInvalidException.class, () -> useCase.execute(RAW_TOKEN, "NewStr0ngPass"));

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(tokenRepository, never()).markUsed(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_WithExpiredToken_ThrowsPasswordResetTokenInvalidException() {
        PasswordResetToken expired = new PasswordResetToken(
                10L, 1L, TOKEN_HASH, Instant.now().minusSeconds(60), null, Instant.now().minusSeconds(2000));
        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(expired));

        assertThrows(PasswordResetTokenInvalidException.class, () -> useCase.execute(RAW_TOKEN, "NewStr0ngPass"));

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_WithAlreadyUsedToken_ThrowsPasswordResetTokenInvalidException() {
        PasswordResetToken used = new PasswordResetToken(
                10L, 1L, TOKEN_HASH, Instant.now().plusSeconds(1800), Instant.now().minusSeconds(60), Instant.now().minusSeconds(120));
        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(used));

        assertThrows(PasswordResetTokenInvalidException.class, () -> useCase.execute(RAW_TOKEN, "NewStr0ngPass"));

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_WithValidTokenButUserSinceDeleted_ThrowsResourceNotFoundException() {
        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(validToken()));
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(RAW_TOKEN, "NewStr0ngPass"));

        verify(tokenRepository, never()).markUsed(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
