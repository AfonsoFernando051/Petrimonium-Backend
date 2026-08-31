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

class GetMyPetUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    private GetMyPetUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetMyPetUseCaseImpl(userRepository);
    }

    @Test
    void execute_WhenUserHasAPet_ReturnsIt() {
        Pet pet = new Pet();
        pet.setName("Rex");
        User user = new User();
        user.setEmail(EMAIL);
        user.setPet(pet);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        Optional<Pet> result = useCase.execute(EMAIL);

        assertTrue(result.isPresent());
        assertEquals("Rex", result.get().getName());
    }

    @Test
    void execute_WhenUserHasNoPetYet_ReturnsEmpty() {
        User user = new User();
        user.setEmail(EMAIL);
        user.setPet(null);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertTrue(useCase.execute(EMAIL).isEmpty());
    }

    @Test
    void execute_WhenUserDoesNotExist_Throws() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL));
    }
}
