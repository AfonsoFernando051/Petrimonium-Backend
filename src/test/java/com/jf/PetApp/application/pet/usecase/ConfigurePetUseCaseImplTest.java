package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.application.pet.port.PetRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import com.jf.PetApp.core.domain.enums.PetSpecieEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private PetRepositoryPort petRepository;

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
        when(petRepository.findByUserIdAndAppContext(any(), eq(AppContextEnum.WALLET))).thenReturn(Optional.empty());

        configurePetUseCase.execute(email, AppContextEnum.WALLET, PetSpecieEnum.DOG, "Rex");

        verify(petRepository, times(1)).saveAndLink(any(), eq(AppContextEnum.WALLET));
    }

    @Test
    void execute_WhenUserDoesNotExist_ShouldThrowException() {
        String email = "notfound@test.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> configurePetUseCase.execute(email, AppContextEnum.WALLET, PetSpecieEnum.DOG, "Rex"));
        verify(petRepository, never()).saveAndLink(any(), any());
    }

    @Test
    void execute_WhenNameIsProvided_UsesItInsteadOfTheGeneratedDefault() {
        String email = "test@test.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(petRepository.findByUserIdAndAppContext(any(), eq(AppContextEnum.HEALTH))).thenReturn(Optional.empty());

        configurePetUseCase.execute(email, AppContextEnum.HEALTH, PetSpecieEnum.FOX, "Rusty");

        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository).saveAndLink(captor.capture(), eq(AppContextEnum.HEALTH));
        assertEquals("Rusty", captor.getValue().getName());
    }

    @Test
    void execute_WhenNameIsProvidedWithSurroundingWhitespace_Trims() {
        String email = "test@test.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(petRepository.findByUserIdAndAppContext(any(), eq(AppContextEnum.HEALTH))).thenReturn(Optional.empty());

        configurePetUseCase.execute(email, AppContextEnum.HEALTH, PetSpecieEnum.FOX, "  Rusty  ");

        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository).saveAndLink(captor.capture(), eq(AppContextEnum.HEALTH));
        assertEquals("Rusty", captor.getValue().getName());
    }

    @Test
    void execute_WhenNameIsNullOrBlank_FallsBackToTheGeneratedDefault() {
        String email = "test@test.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(petRepository.findByUserIdAndAppContext(any(), eq(AppContextEnum.HEALTH))).thenReturn(Optional.empty());

        configurePetUseCase.execute(email, AppContextEnum.HEALTH, PetSpecieEnum.FOX, null);

        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository).saveAndLink(captor.capture(), eq(AppContextEnum.HEALTH));
        assertEquals("FOX Companion", captor.getValue().getName());

        User user2 = new User();
        user2.setEmail("test2@test.com");
        when(userRepository.findByEmail("test2@test.com")).thenReturn(Optional.of(user2));
        when(petRepository.findByUserIdAndAppContext(any(), eq(AppContextEnum.WALLET))).thenReturn(Optional.empty());

        configurePetUseCase.execute("test2@test.com", AppContextEnum.WALLET, PetSpecieEnum.DOG, "   ");

        ArgumentCaptor<Pet> captor2 = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository).saveAndLink(captor2.capture(), eq(AppContextEnum.WALLET));
        assertEquals("DOG Companion", captor2.getValue().getName());
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

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(petRepository.findByUserIdAndAppContext(any(), eq(AppContextEnum.WALLET)))
                .thenReturn(Optional.of(existingPet));

        configurePetUseCase.execute(email, AppContextEnum.WALLET, PetSpecieEnum.CAT, "New Name Attempt");

        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository).saveAndLink(captor.capture(), eq(AppContextEnum.WALLET));
        assertEquals("Original Name", captor.getValue().getName());
        assertEquals(PetSpecieEnum.CAT, captor.getValue().getSpecie());
    }
}
