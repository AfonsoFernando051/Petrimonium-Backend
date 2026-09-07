package com.jf.PetApp.infrastructure.controller.settings;

import com.jf.PetApp.application.settings.usecase.DeleteAccountUseCase;
import com.jf.PetApp.application.settings.usecase.UpdateCountryUseCase;
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
    private final UpdateCountryUseCase updateCountryUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;

    public SettingsController(UserRepository userRepository,
                              UpdateLanguageUseCase updateLanguageUseCase,
                              UpdateCountryUseCase updateCountryUseCase,
                              DeleteAccountUseCase deleteAccountUseCase) {
        this.userRepository = userRepository;
        this.updateLanguageUseCase = updateLanguageUseCase;
        this.updateCountryUseCase = updateCountryUseCase;
        this.deleteAccountUseCase = deleteAccountUseCase;
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

    @GetMapping("/country")
    public ResponseEntity<CountryResponseDTO> getCountry() {
        User user = userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        // Pode vir null: significa que a conta ainda não escolheu país.
        return ResponseEntity.ok(new CountryResponseDTO(user.getCountryCode()));
    }

    @PutMapping("/country")
    public ResponseEntity<CountryResponseDTO> updateCountry(@RequestBody UpdateCountryRequestDTO request) {
        String email = SecurityUtils.getCurrentUserEmail();
        String updatedCountry = updateCountryUseCase.execute(email, request.countryCode());
        return ResponseEntity.ok(new CountryResponseDTO(updatedCountry));
    }

    /**
     * Irreversível: apaga a conta e todos os dados dela em todos os contextos.
     * Não há período de carência. Os tokens saem junto, por isso o pedido
     * seguinte deste cliente responde 401 — é o esperado.
     */
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount() {
        deleteAccountUseCase.execute(SecurityUtils.getCurrentUserEmail());
        return ResponseEntity.noContent().build();
    }

    public record LanguageResponseDTO(String language) {}
    public record UpdateLanguageRequestDTO(String language) {}
    public record CountryResponseDTO(String countryCode) {}
    public record UpdateCountryRequestDTO(String countryCode) {}
}
