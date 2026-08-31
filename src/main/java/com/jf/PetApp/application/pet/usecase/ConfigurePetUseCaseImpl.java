package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.PetSpecieEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigurePetUseCaseImpl implements ConfigurePetUseCase {

    private static final int DEFAULT_PET_HEALTH = 100;

    private final UserRepository userRepository;

    public ConfigurePetUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void execute(String userEmail, PetSpecieEnum specie) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Pet pet = user.getPet();
        if (pet == null) {
            pet = new Pet();
            pet.setUser(user);
            pet.setHealth(DEFAULT_PET_HEALTH);
            pet.setName(specie.name() + " Companion");
            user.setPet(pet);
        }
        pet.setSpecie(specie);
        
        userRepository.save(user);
    }
}
