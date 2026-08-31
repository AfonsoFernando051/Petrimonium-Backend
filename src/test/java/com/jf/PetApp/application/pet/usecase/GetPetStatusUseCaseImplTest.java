package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
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

    private GetPetStatusUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetPetStatusUseCaseImpl(userRepository);
    }

    @Test
    void execute_WhenUserHasAPet_ReturnsTrue() {
        User user = new User();
        user.setPet(new Pet());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertTrue(useCase.execute(EMAIL));
    }

    @Test
    void execute_WhenUserHasNoPet_ReturnsFalse() {
        User user = new User();
        user.setPet(null);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertFalse(useCase.execute(EMAIL));
    }

    @Test
    void execute_WhenUserDoesNotExist_Throws() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL));
    }
}
