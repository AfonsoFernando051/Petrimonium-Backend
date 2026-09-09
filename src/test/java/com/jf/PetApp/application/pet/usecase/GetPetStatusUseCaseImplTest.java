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

class GetPetStatusUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetRepositoryPort petRepository;

    private GetPetStatusUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";
    private static final Long USER_ID = 7L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetPetStatusUseCaseImpl(userRepository, petRepository);
    }

    @Test
    void execute_WhenUserHasAPet_ReturnsTrue() {
        User user = new User();
        user.setId(USER_ID);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(petRepository.findByUserIdAndAppContext(USER_ID, AppContextEnum.WALLET))
                .thenReturn(Optional.of(new Pet()));

        assertTrue(useCase.execute(EMAIL, AppContextEnum.WALLET));
    }

    @Test
    void execute_WhenUserHasNoPet_ReturnsFalse() {
        User user = new User();
        user.setId(USER_ID);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(petRepository.findByUserIdAndAppContext(USER_ID, AppContextEnum.WALLET)).thenReturn(Optional.empty());

        assertFalse(useCase.execute(EMAIL, AppContextEnum.WALLET));
    }

    @Test
    void execute_WhenUserDoesNotExist_Throws() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL, AppContextEnum.WALLET));
    }

    @Test
    void execute_WhenAppContextIsNull_ReturnsFalseWithoutLookingUpTheUser() {
        assertFalse(useCase.execute(EMAIL, null));
        org.mockito.Mockito.verifyNoInteractions(userRepository, petRepository);
    }
}
