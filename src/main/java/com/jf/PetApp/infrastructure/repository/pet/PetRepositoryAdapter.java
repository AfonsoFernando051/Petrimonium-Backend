package com.jf.PetApp.infrastructure.repository.pet;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.pet.port.PetRepositoryPort;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import com.jf.PetApp.infrastructure.entity.PetAppLinkJpaEntity;
import com.jf.PetApp.infrastructure.entity.PetJpaEntity;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;
import com.jf.PetApp.infrastructure.repository.PetRepository;
import com.jf.PetApp.infrastructure.repository.user.SpringUserJpaRepository;

@Repository
public class PetRepositoryAdapter implements PetRepositoryPort {

    private final PetRepository petJpaRepository;
    private final PetAppLinkRepository linkRepository;
    private final SpringUserJpaRepository userJpaRepository;

    public PetRepositoryAdapter(
            PetRepository petJpaRepository,
            PetAppLinkRepository linkRepository,
            SpringUserJpaRepository userJpaRepository) {
        this.petJpaRepository = petJpaRepository;
        this.linkRepository = linkRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Optional<Pet> findByUserIdAndAppContext(Long userId, AppContextEnum appContext) {
        return linkRepository.findByUser_IdAndAppContext(userId, appContext)
                .map(link -> link.getPet().toDomain(null));
    }

    @Override
    public Optional<Pet> findById(Integer petId) {
        return petJpaRepository.findById(petId).map(entity -> {
            // Just enough of the owner to let a caller check ownership (pet.getUser().getId()) —
            // entity.getUser() is a lazy reference already loaded with the pet, so reading its id
            // needs no extra query.
            User owner = new User();
            owner.setId(entity.getUser().getId());
            return entity.toDomain(owner);
        });
    }

    @Override
    public List<Pet> findAllByUserId(Long userId) {
        return petJpaRepository.findAllByUser_Id(userId).stream()
                .map(entity -> entity.toDomain(null))
                .toList();
    }

    @Override
    @Transactional
    public Pet saveAndLink(Pet pet, AppContextEnum appContext) {
        // Callers (the pet use cases) already resolve the User through UserRepository before
        // reaching here, so a missing row at this point is a genuine bug, not a normal-flow
        // validation case — deliberately not translated to IllegalArgumentException: this class is
        // @Repository, and Spring's persistence-exception-translation AOP would otherwise re-wrap
        // it into a DataAccessException that the controller's `catch (IllegalArgumentException)`
        // no longer matches, turning an intended 400 into an opaque 500 (see LinkPetToAppUseCaseImpl
        // for where "pet/user not found" is actually validated and turned into a real 400).
        Long userId = pet.getUser().getId();
        UserJpaEntity userRef = userJpaRepository.findById(Math.toIntExact(userId)).orElseThrow();

        PetJpaEntity petEntity = PetJpaEntity.fromDomain(pet);
        petEntity.setUser(userRef);
        PetJpaEntity savedPet = petJpaRepository.save(petEntity);

        PetAppLinkJpaEntity link = linkRepository.findByUser_IdAndAppContext(userId, appContext)
                .orElseGet(PetAppLinkJpaEntity::new);
        link.setUser(userRef);
        link.setAppContext(appContext);
        link.setPet(savedPet);
        linkRepository.save(link);

        return savedPet.toDomain(pet.getUser());
    }

    @Override
    @Transactional
    public void link(Integer petId, Long userId, AppContextEnum appContext) {
        // Ownership is already validated by LinkPetToAppUseCaseImpl before this is called — see
        // saveAndLink's comment for why this deliberately doesn't throw IllegalArgumentException.
        PetJpaEntity pet = petJpaRepository.findById(petId).orElseThrow();
        UserJpaEntity userRef = userJpaRepository.findById(Math.toIntExact(userId)).orElseThrow();

        PetAppLinkJpaEntity link = linkRepository.findByUser_IdAndAppContext(userId, appContext)
                .orElseGet(PetAppLinkJpaEntity::new);
        link.setUser(userRef);
        link.setAppContext(appContext);
        link.setPet(pet);
        linkRepository.save(link);
    }

    @Override
    @Transactional
    public void deleteAllByUserId(Long userId) {
        linkRepository.deleteByUser_Id(userId);
        petJpaRepository.deleteByUser_Id(userId);
    }
}
