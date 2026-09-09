package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.application.pet.port.PetRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListMyPetsUseCaseImpl implements ListMyPetsUseCase {

    private final UserRepository userRepository;
    private final PetRepositoryPort petRepository;

    public ListMyPetsUseCaseImpl(UserRepository userRepository, PetRepositoryPort petRepository) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
    }

    @Override
    public List<Pet> execute(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return petRepository.findAllByUserId(user.getId());
    }
}
