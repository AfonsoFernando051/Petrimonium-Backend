package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.application.pet.port.PetRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkPetToAppUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetRepositoryPort petRepository;

    private LinkPetToAppUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";
    private static final Long USER_ID = 7L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new LinkPetToAppUseCaseImpl(userRepository, petRepository);
    }

    @Test
    void execute_WhenPetBelongsToTheUser_LinksIt() {
        User user = new User();
        user.setId(USER_ID);
        User owner = new User();
        owner.setId(USER_ID);
        Pet pet = new Pet();
        pet.setId(3);
        pet.setUser(owner);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(petRepository.findById(3)).thenReturn(Optional.of(pet));

        useCase.execute(EMAIL, 3, AppContextEnum.WALLET);

        verify(petRepository).link(3, USER_ID, AppContextEnum.WALLET);
    }

    @Test
    void execute_WhenPetIdDoesNotExist_ThrowsInsteadOfCallingLink() {
        User user = new User();
        user.setId(USER_ID);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(petRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL, 999, AppContextEnum.WALLET));
        verify(petRepository, never()).link(any(), any(), any());
    }

    @Test
    void execute_WhenPetBelongsToAnotherUser_ThrowsInsteadOfCallingLink() {
        User user = new User();
        user.setId(USER_ID);
        User someoneElse = new User();
        someoneElse.setId(USER_ID + 1);
        Pet pet = new Pet();
        pet.setId(3);
        pet.setUser(someoneElse);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(petRepository.findById(3)).thenReturn(Optional.of(pet));

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class, () -> useCase.execute(EMAIL, 3, AppContextEnum.WALLET));
        assertEquals("Pet not found for this user", thrown.getMessage());
        verify(petRepository, never()).link(any(), any(), any());
    }

    @Test
    void execute_WhenUserDoesNotExist_Throws() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL, 3, AppContextEnum.WALLET));
        verify(petRepository, never()).link(any(), any(), any());
    }
}
