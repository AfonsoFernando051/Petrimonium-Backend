package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.application.pet.port.PetRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import com.jf.PetApp.core.domain.enums.PetSpecieEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigurePetUseCaseImpl implements ConfigurePetUseCase {

    private static final int DEFAULT_PET_HEALTH = 100;

    private final UserRepository userRepository;
    private final PetRepositoryPort petRepository;

    public ConfigurePetUseCaseImpl(UserRepository userRepository, PetRepositoryPort petRepository) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
    }

    @Override
    @Transactional
    public void execute(String userEmail, AppContextEnum appContext, PetSpecieEnum specie, String name) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Pet pet = petRepository.findByUserIdAndAppContext(user.getId(), appContext).orElse(null);
        if (pet == null) {
            pet = new Pet();
            pet.setUser(user);
            pet.setHealth(DEFAULT_PET_HEALTH);
            pet.setName(resolveName(name, specie));
        }
        pet.setSpecie(specie);

        petRepository.saveAndLink(pet, appContext);
    }

    private String resolveName(String name, PetSpecieEnum specie) {
        String trimmed = name == null ? null : name.trim();
        return trimmed == null || trimmed.isEmpty() ? specie.name() + " Companion" : trimmed;
    }
}
