package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.application.pet.port.PetRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class GetMyPetUseCaseImpl implements GetMyPetUseCase {

    private final UserRepository userRepository;
    private final PetRepositoryPort petRepository;

    public GetMyPetUseCaseImpl(UserRepository userRepository, PetRepositoryPort petRepository) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
    }

    @Override
    public Optional<Pet> execute(String userEmail, AppContextEnum appContext) {
        if (appContext == null) {
            return Optional.empty();
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return petRepository.findByUserIdAndAppContext(user.getId(), appContext);
    }
}
