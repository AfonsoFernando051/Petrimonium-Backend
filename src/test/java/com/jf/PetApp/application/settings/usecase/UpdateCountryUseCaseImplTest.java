package com.jf.PetApp.application.settings.usecase;

import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateCountryUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UpdateCountryUseCaseImpl updateCountryUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_WithSupportedCountry_ShouldUpdateAndPersist() {
        String email = "user@test.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        String result = updateCountryUseCase.execute(email, "PT");

        assertEquals("PT", result);
        assertEquals("PT", user.getCountryCode());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void execute_WithLowercaseCountry_ShouldStoreUppercase() {
        String email = "user@test.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertEquals("BR", updateCountryUseCase.execute(email, "br"));
    }

    @Test
    void execute_WithUnsupportedCountry_ShouldThrowWithoutTouchingRepository() {
        // O ecossistema opera só em BR e PT nesta fase; aceitar qualquer
        // ISO deixaria entrar país sem moeda nem conteúdo suportados.
        assertThrows(IllegalArgumentException.class, () ->
            updateCountryUseCase.execute("user@test.com", "US"));

        verifyNoInteractions(userRepository);
    }

    @Test
    void execute_WithNullCountry_ShouldThrowWithoutTouchingRepository() {
        assertThrows(IllegalArgumentException.class, () ->
            updateCountryUseCase.execute("user@test.com", null));

        verifyNoInteractions(userRepository);
    }

    @Test
    void execute_WhenUserDoesNotExist_ShouldThrow() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
            updateCountryUseCase.execute("missing@test.com", "BR"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void newUser_HasNoCountryUntilChosen() {
        // Nulo é "ainda não escolheu". Não se infere país do dispositivo.
        assertNull(new User().getCountryCode());
    }
}
