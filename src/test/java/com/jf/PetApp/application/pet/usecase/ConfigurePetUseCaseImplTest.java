package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.PetSpecieEnum;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;
import com.jf.PetApp.infrastructure.repository.PetRepository;
import com.jf.PetApp.application.user.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ConfigurePetUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConfigurePetUseCaseImpl configurePetUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_WhenUserExists_ShouldSavePet() {
        String email = "test@test.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        configurePetUseCase.execute(email, PetSpecieEnum.DOG);

        verify(userRepository, times(1)).save(any());
    }

    @Test
    void execute_WhenUserDoesNotExist_ShouldThrowException() {
        String email = "notfound@test.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> configurePetUseCase.execute(email, PetSpecieEnum.DOG));
        verify(userRepository, never()).save(any());
    }
}
