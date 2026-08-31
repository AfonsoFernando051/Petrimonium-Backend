package com.jf.PetApp.application.auth.usecase;

import com.jf.PetApp.application.auth.port.PasswordResetMailerPort;
import com.jf.PetApp.application.auth.port.PasswordResetTokenRepositoryPort;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RequestPasswordResetUseCaseImplTest {

    private static final String EMAIL = "investor@test.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepositoryPort tokenRepository;

    @Mock
    private PasswordResetMailerPort mailerPort;

    private RequestPasswordResetUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new RequestPasswordResetUseCaseImpl(userRepository, tokenRepository, mailerPort);
    }

    private User userWith(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    @Test
    void execute_WithExistingEmail_InvalidatesPriorTokensAndStoresAHashedNewOne() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userWith(1L, EMAIL)));

        useCase.execute(EMAIL);

        verify(tokenRepository).invalidateOutstandingForUser(1L);

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();
        assertEquals(1L, saved.userId());
        assertNotNull(saved.tokenHash());
        assertTrue(saved.expiresAt().isAfter(Instant.now()));
        assertEquals(null, saved.usedAt());
    }

    @Test
    void execute_WithExistingEmail_EmailsTheRawTokenNeverTheHash() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userWith(1L, EMAIL)));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        useCase.execute(EMAIL);
        verify(tokenRepository).save(tokenCaptor.capture());

        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailerPort).sendPasswordResetEmail(eq(EMAIL), rawTokenCaptor.capture());

        String storedHash = tokenCaptor.getValue().tokenHash();
        String emailedRawToken = rawTokenCaptor.getValue();
        // The raw token must never equal what's persisted — only its hash is stored.
        assertTrue(!emailedRawToken.equals(storedHash));
        assertEquals(storedHash, PasswordResetToken.hash(emailedRawToken));
    }

    @Test
    void execute_WithUnknownEmail_SilentlyNoOpsWithoutRevealingAccountExistence() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        useCase.execute("nobody@test.com");

        verifyNoInteractions(tokenRepository, mailerPort);
    }

    @Test
    void execute_CalledTwiceForSameUser_InvalidatesOnEachCallSoOnlyTheNewestTokenStaysRedeemable() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userWith(1L, EMAIL)));

        useCase.execute(EMAIL);
        useCase.execute(EMAIL);

        verify(tokenRepository, times(2)).invalidateOutstandingForUser(1L);
        verify(tokenRepository, times(2)).save(any());
    }

    @Test
    void execute_GeneratesADifferentRawTokenOnEachCall() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userWith(1L, EMAIL)));

        ArgumentCaptor<String> tokens = ArgumentCaptor.forClass(String.class);
        useCase.execute(EMAIL);
        useCase.execute(EMAIL);
        verify(mailerPort, times(2)).sendPasswordResetEmail(eq(EMAIL), tokens.capture());

        assertTrue(!tokens.getAllValues().get(0).equals(tokens.getAllValues().get(1)));
    }
}
