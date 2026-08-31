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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateLanguageUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UpdateLanguageUseCaseImpl updateLanguageUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_WithSupportedLanguage_ShouldUpdateAndPersist() {
        String email = "user@test.com";
        User user = new User();
        user.setEmail(email);
        user.setPreferredLanguage("pt");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        String result = updateLanguageUseCase.execute(email, "en");

        assertEquals("en", result);
        assertEquals("en", user.getPreferredLanguage());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void execute_WithUnsupportedLanguage_ShouldThrowWithoutTouchingRepository() {
        assertThrows(IllegalArgumentException.class, () ->
            updateLanguageUseCase.execute("user@test.com", "fr"));

        verifyNoInteractions(userRepository);
    }

    @Test
    void execute_WhenUserDoesNotExist_ShouldThrow() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
            updateLanguageUseCase.execute("missing@test.com", "en"));

        verify(userRepository, never()).save(any());
    }
}
