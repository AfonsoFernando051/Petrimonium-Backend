package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import org.springframework.stereotype.Service;

@Service
public class GetPetStatusUseCaseImpl implements GetPetStatusUseCase {

    private final UserRepository userRepository;

    public GetPetStatusUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean execute(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return user.getPet() != null;
    }
}
