package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class GetMyPetUseCaseImpl implements GetMyPetUseCase {

    private final UserRepository userRepository;

    public GetMyPetUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<Pet> execute(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return Optional.ofNullable(user.getPet());
    }
}
