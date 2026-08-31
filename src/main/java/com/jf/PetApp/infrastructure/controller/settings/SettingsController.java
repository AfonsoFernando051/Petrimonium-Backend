package com.jf.PetApp.infrastructure.controller.settings;

import com.jf.PetApp.application.settings.usecase.UpdateLanguageUseCase;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final UserRepository userRepository;
    private final UpdateLanguageUseCase updateLanguageUseCase;

    public SettingsController(UserRepository userRepository, UpdateLanguageUseCase updateLanguageUseCase) {
        this.userRepository = userRepository;
        this.updateLanguageUseCase = updateLanguageUseCase;
    }

    @GetMapping("/language")
    public ResponseEntity<LanguageResponseDTO> getLanguage() {
        User user = userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return ResponseEntity.ok(new LanguageResponseDTO(user.getPreferredLanguage()));
    }

    @PutMapping("/language")
    public ResponseEntity<LanguageResponseDTO> updateLanguage(@RequestBody UpdateLanguageRequestDTO request) {
        String email = SecurityUtils.getCurrentUserEmail();
        String updatedLanguage = updateLanguageUseCase.execute(email, request.language());
        return ResponseEntity.ok(new LanguageResponseDTO(updatedLanguage));
    }

    public record LanguageResponseDTO(String language) {}
    public record UpdateLanguageRequestDTO(String language) {}
}
