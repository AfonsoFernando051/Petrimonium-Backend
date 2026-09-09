package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.application.pet.port.PetRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkPetToAppUseCaseImpl implements LinkPetToAppUseCase {

    private final UserRepository userRepository;
    private final PetRepositoryPort petRepository;

    public LinkPetToAppUseCaseImpl(UserRepository userRepository, PetRepositoryPort petRepository) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
    }

    @Override
    @Transactional
    public void execute(String userEmail, Integer petId, AppContextEnum appContext) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Validated here, in the @Service layer, rather than inside PetRepositoryAdapter: that
        // class is @Repository, and an IllegalArgumentException thrown from within a @Repository
        // bean's own method gets caught and re-wrapped by Spring's persistence-exception-translation
        // AOP into a DataAccessException — which PetController's `catch (IllegalArgumentException)`
        // no longer matches, turning this into an opaque 500 instead of the intended 400.
        Pet pet = petRepository.findById(petId)
                .filter(p -> p.getUser() != null && user.getId().equals(p.getUser().getId()))
                .orElseThrow(() -> new IllegalArgumentException("Pet not found for this user"));

        petRepository.link(pet.getId(), user.getId(), appContext);
    }
}
