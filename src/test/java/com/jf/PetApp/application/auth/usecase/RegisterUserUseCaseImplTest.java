package com.jf.PetApp.application.auth.usecase;

import com.jf.PetApp.application.auth.dto.RegisterCommand;
import com.jf.PetApp.application.auth.dto.RegisterResult;
import com.jf.PetApp.application.auth.exception.UserAlreadyExistsException;
import com.jf.PetApp.application.auth.port.PasswordEncoderPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.RoleEnum;
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

class RegisterUserUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    @InjectMocks
    private RegisterUserUseCaseImpl registerUserUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_WithNewEmail_ShouldHashPasswordAndPersistUser() {
        String email = "newinvestor@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Str0ngPass")).thenReturn("hashed-Str0ngPass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        RegisterResult result = registerUserUseCase.execute(new RegisterCommand("newinvestor", email, "Str0ngPass"));

        assertEquals(42L, result.userId());
        assertEquals("newinvestor", result.username());
        assertEquals(email, result.email());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();
        // The plaintext password must never reach the repository — only the hash does.
        assertEquals("hashed-Str0ngPass", persisted.getPassword());
        assertEquals(RoleEnum.USER, persisted.getRole());
    }

    @Test
    void execute_WithAlreadyRegisteredEmail_ShouldThrowWithoutPersistingOrHashing() {
        String email = "existing@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(new User()));

        assertThrows(UserAlreadyExistsException.class, () ->
            registerUserUseCase.execute(new RegisterCommand("someone", email, "Str0ngPass")));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
}
