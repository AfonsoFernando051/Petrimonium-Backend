package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
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
        // class is @Repository, and an exception thrown from within a @Repository bean's own
        // method gets caught and re-wrapped by Spring's persistence-exception-translation AOP into
        // a DataAccessException — which GlobalExceptionHandler's specific handlers no longer match,
        // turning this into an opaque 500 instead of the intended 404.
        //
        // A missing id and a pet owned by someone else look identical to the caller on purpose —
        // 404 either way, same as Investment/Health do for "not found or not yours" — so a probe
        // for other users' pet ids can't distinguish "doesn't exist" from "exists but not yours".
        Pet pet = petRepository.findById(petId)
                .filter(p -> p.getUser() != null && user.getId().equals(p.getUser().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found for this user"));

        petRepository.link(pet.getId(), user.getId(), appContext);
    }
}
