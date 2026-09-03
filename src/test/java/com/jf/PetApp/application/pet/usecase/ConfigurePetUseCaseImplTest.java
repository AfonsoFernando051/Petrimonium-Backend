package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.core.domain.Pet;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        configurePetUseCase.execute(email, PetSpecieEnum.DOG, "Rex");

        verify(userRepository, times(1)).save(any());
    }

    @Test
    void execute_WhenUserDoesNotExist_ShouldThrowException() {
        String email = "notfound@test.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class, () -> configurePetUseCase.execute(email, PetSpecieEnum.DOG, "Rex"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_WhenNameIsProvided_UsesItInsteadOfTheGeneratedDefault() {
        String email = "test@test.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        configurePetUseCase.execute(email, PetSpecieEnum.FOX, "Rusty");

        assertEquals("Rusty", user.getPet().getName());
    }

    @Test
    void execute_WhenNameIsProvidedWithSurroundingWhitespace_Trims() {
        String email = "test@test.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        configurePetUseCase.execute(email, PetSpecieEnum.FOX, "  Rusty  ");

        assertEquals("Rusty", user.getPet().getName());
    }

    @Test
    void execute_WhenNameIsNullOrBlank_FallsBackToTheGeneratedDefault() {
        String email = "test@test.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        configurePetUseCase.execute(email, PetSpecieEnum.FOX, null);

        assertEquals("FOX Companion", user.getPet().getName());

        User user2 = new User();
        user2.setEmail("test2@test.com");
        when(userRepository.findByEmail("test2@test.com")).thenReturn(Optional.of(user2));

        configurePetUseCase.execute("test2@test.com", PetSpecieEnum.DOG, "   ");

        assertEquals("DOG Companion", user2.getPet().getName());
    }

    @Test
    void execute_WhenPetAlreadyExists_DoesNotOverwriteItsName() {
        String email = "test@test.com";
        User user = new User();
        user.setEmail(email);
        Pet existingPet = new Pet();
        existingPet.setUser(user);
        existingPet.setName("Original Name");
        existingPet.setSpecie(PetSpecieEnum.DOG);
        user.setPet(existingPet);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        configurePetUseCase.execute(email, PetSpecieEnum.CAT, "New Name Attempt");

        assertEquals("Original Name", user.getPet().getName());
        assertEquals(PetSpecieEnum.CAT, user.getPet().getSpecie());
    }
}
