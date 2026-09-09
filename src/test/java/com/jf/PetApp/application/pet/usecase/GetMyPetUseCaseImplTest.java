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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GetMyPetUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetRepositoryPort petRepository;

    private GetMyPetUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";
    private static final Long USER_ID = 7L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetMyPetUseCaseImpl(userRepository, petRepository);
    }

    @Test
    void execute_WhenUserHasAPet_ReturnsIt() {
        Pet pet = new Pet();
        pet.setName("Rex");
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(petRepository.findByUserIdAndAppContext(USER_ID, AppContextEnum.WALLET)).thenReturn(Optional.of(pet));

        Optional<Pet> result = useCase.execute(EMAIL, AppContextEnum.WALLET);

        assertTrue(result.isPresent());
        assertEquals("Rex", result.get().getName());
    }

    @Test
    void execute_WhenUserHasNoPetYet_ReturnsEmpty() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(petRepository.findByUserIdAndAppContext(USER_ID, AppContextEnum.WALLET)).thenReturn(Optional.empty());

        assertTrue(useCase.execute(EMAIL, AppContextEnum.WALLET).isEmpty());
    }

    @Test
    void execute_WhenUserDoesNotExist_Throws() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL, AppContextEnum.WALLET));
    }

    @Test
    void execute_WhenAppContextIsNull_ReturnsEmptyWithoutLookingUpTheUser() {
        assertTrue(useCase.execute(EMAIL, null).isEmpty());
        org.mockito.Mockito.verifyNoInteractions(userRepository, petRepository);
    }
}
