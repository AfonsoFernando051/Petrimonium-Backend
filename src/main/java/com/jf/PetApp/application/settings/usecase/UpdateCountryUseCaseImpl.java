package com.jf.PetApp.application.settings.usecase;

import com.jf.PetApp.application.settings.AccountPreferences;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UpdateCountryUseCaseImpl implements UpdateCountryUseCase {

    private final UserRepository userRepository;

    public UpdateCountryUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String execute(String email, String countryCode) {
        String normalized = AccountPreferences.normalizeCountry(countryCode);
        if (!AccountPreferences.isSupportedCountry(normalized)) {
            throw new IllegalArgumentException("Unsupported country");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        user.setCountryCode(normalized);
        userRepository.save(user);

        return user.getCountryCode();
    }
}
