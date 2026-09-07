package com.jf.PetApp.application.settings.usecase;

import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.infrastructure.repository.user.UserDataEraser;
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
    private UserDataEraser userDataEraser;

    @InjectMocks
    private DeleteAccountUseCaseImpl deleteAccountUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private User user(long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    @Test
    void execute_ErasesEveryContextBeforeRemovingTheAccountRow() {
        // A ordem é o que importa: apagar jf_users primeiro deixaria as filhas
        // a violar chave estrangeira, ou órfãs onde a FK não existe.
        User user = user(7L, "user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        deleteAccountUseCase.execute("user@test.com");

        InOrder order = inOrder(userDataEraser, userRepository);
        order.verify(userDataEraser).eraseAll(7L, "user@test.com");
        order.verify(userRepository).delete(user);
    }

    @Test
    void execute_WhenUserDoesNotExist_ShouldThrowWithoutErasingAnything() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
            deleteAccountUseCase.execute("missing@test.com"));

        verifyNoInteractions(userDataEraser);
        verify(userRepository, never()).delete(any());
    }
}
