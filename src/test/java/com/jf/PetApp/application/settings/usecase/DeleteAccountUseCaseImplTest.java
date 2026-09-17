package com.jf.PetApp.application.settings.usecase;

import com.jf.PetApp.application.auth.exception.AuthenticationException;
import com.jf.PetApp.application.auth.port.GoogleTokenVerifierPort;
import com.jf.PetApp.application.auth.port.GoogleUserInfo;
import com.jf.PetApp.application.auth.port.PasswordEncoderPort;
import com.jf.PetApp.application.settings.dto.DeleteAccountCommand;
import com.jf.PetApp.application.user.port.UserDataErasurePort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeleteAccountUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDataErasurePort userDataEraser;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @Mock
    private GoogleTokenVerifierPort googleTokenVerifier;

    @InjectMocks
    private DeleteAccountUseCaseImpl deleteAccountUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private User localUser(long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword("hashed-password");
        return user;
    }

    private User googleUser(long id, String email, String providerId) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword(null);
        user.setProviderId(providerId);
        return user;
    }

    private DeleteAccountCommand withPassword(String email, String password) {
        return new DeleteAccountCommand(email, password, null);
    }

    private DeleteAccountCommand withGoogleToken(String email, String idToken) {
        return new DeleteAccountCommand(email, null, idToken);
    }

    @Test
    void execute_ErasesEveryContextBeforeRemovingTheAccountRow() {
        // A ordem é o que importa: apagar jf_users primeiro deixaria as filhas
        // a violar chave estrangeira, ou órfãs onde a FK não existe.
        User user = localUser(7L, "user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);

        deleteAccountUseCase.execute(withPassword("user@test.com", "correct-password"));

        InOrder order = inOrder(userDataEraser, userRepository);
        order.verify(userDataEraser).eraseAll(7L, "user@test.com");
        order.verify(userRepository).delete(user);
    }

    @Test
    void execute_WhenUserDoesNotExist_ShouldThrowWithoutErasingAnything() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
            deleteAccountUseCase.execute(withPassword("missing@test.com", "any-password")));

        verifyNoInteractions(userDataEraser);
        verify(userRepository, never()).delete(any());
    }

    /**
     * The bearer token alone used to be enough here. That made the one irreversible operation in
     * the product cheaper to reach than replacing a portfolio, which already demands an explicit
     * confirmation — a stolen access token is valid for a full hour and needs no user interaction.
     */
    @Test
    void execute_WithWrongPassword_RefusesAndErasesNothing() {
        User user = localUser(7L, "user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(AuthenticationException.class, () ->
            deleteAccountUseCase.execute(withPassword("user@test.com", "wrong-password")));

        verifyNoInteractions(userDataEraser);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void execute_WithNoCredentialAtAll_RefusesAndErasesNothing() {
        User user = localUser(7L, "user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        assertThrows(AuthenticationException.class, () ->
            deleteAccountUseCase.execute(new DeleteAccountCommand("user@test.com", "   ", null)));

        verifyNoInteractions(userDataEraser);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void execute_ForAGoogleAccount_AcceptsAFreshIdTokenForThatSameGoogleIdentity() {
        User user = googleUser(9L, "user@test.com", "google-sub-123");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(googleTokenVerifier.verify("fresh-id-token"))
                .thenReturn(new GoogleUserInfo("google-sub-123", "user@test.com", "User"));

        deleteAccountUseCase.execute(withGoogleToken("user@test.com", "fresh-id-token"));

        verify(userDataEraser).eraseAll(9L, "user@test.com");
        verify(userRepository).delete(user);
    }

    /**
     * The check that makes the Google path safe at all: a valid token proves the bearer owns
     * *some* Google account, not this one. Without binding it to the account being deleted, any
     * attacker with their own Google account could pair their own token with a stolen bearer
     * token and delete someone else's data.
     */
    @Test
    void execute_ForAGoogleAccount_RefusesATokenBelongingToADifferentGoogleIdentity() {
        User user = googleUser(9L, "user@test.com", "google-sub-123");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(googleTokenVerifier.verify("attacker-id-token"))
                .thenReturn(new GoogleUserInfo("google-sub-999", "attacker@test.com", "Attacker"));

        assertThrows(AuthenticationException.class, () ->
            deleteAccountUseCase.execute(withGoogleToken("user@test.com", "attacker-id-token")));

        verifyNoInteractions(userDataEraser);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void execute_ForAGoogleAccount_RefusesAnInvalidIdToken() {
        User user = googleUser(9L, "user@test.com", "google-sub-123");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(googleTokenVerifier.verify("bogus")).thenThrow(new AuthenticationException("Invalid Google token"));

        assertThrows(AuthenticationException.class, () ->
            deleteAccountUseCase.execute(withGoogleToken("user@test.com", "bogus")));

        verifyNoInteractions(userDataEraser);
        verify(userRepository, never()).delete(any());
    }

    /**
     * A Google-created account has no password, so the password path must not become a way in
     * for it — {@code passwordEncoder.matches} against a null hash must never be what decides.
     */
    @Test
    void execute_ForAGoogleAccount_RefusesAPasswordSinceThereIsNoneToVerify() {
        User user = googleUser(9L, "user@test.com", "google-sub-123");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        assertThrows(AuthenticationException.class, () ->
            deleteAccountUseCase.execute(withPassword("user@test.com", "anything")));

        verifyNoInteractions(userDataEraser, passwordEncoder);
        verify(userRepository, never()).delete(any());
    }

    /**
     * An account created locally and later linked to Google keeps its password, so both paths
     * stay open to it — the same two ways it can log in.
     */
    @Test
    void execute_ForALinkedAccount_AcceptsEitherCredential() {
        User user = localUser(7L, "user@test.com");
        user.setProviderId("google-sub-123");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(googleTokenVerifier.verify("fresh-id-token"))
                .thenReturn(new GoogleUserInfo("google-sub-123", "user@test.com", "User"));

        deleteAccountUseCase.execute(withGoogleToken("user@test.com", "fresh-id-token"));

        verify(userDataEraser).eraseAll(7L, "user@test.com");
    }
}
