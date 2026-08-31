package com.jf.PetApp.application.settings.usecase;

import com.jf.PetApp.application.translation.service.TranslationCacheService;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UpdateLanguageUseCaseImpl implements UpdateLanguageUseCase {

    private final UserRepository userRepository;

    public UpdateLanguageUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String execute(String email, String language) {
        if (!TranslationCacheService.SUPPORTED_LANGUAGES.contains(language)) {
            throw new IllegalArgumentException("Unsupported language");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        user.setPreferredLanguage(language);
        userRepository.save(user);

        return user.getPreferredLanguage();
    }
}
