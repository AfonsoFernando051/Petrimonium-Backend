package com.jf.PetApp.infrastructure.repository.pet;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.pet.port.PetRepositoryPort;
import com.jf.PetApp.core.domain.Pet;
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
    public List<Pet> findAllByUserId(Long userId) {
        return petJpaRepository.findAllByUser_Id(userId).stream()
                .map(entity -> entity.toDomain(null))
                .toList();
    }

    @Override
    @Transactional
    public Pet saveAndLink(Pet pet, AppContextEnum appContext) {
        Long userId = pet.getUser().getId();
        UserJpaEntity userRef = userJpaRepository.findById(Math.toIntExact(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

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
        PetJpaEntity pet = petJpaRepository.findById(petId)
                .filter(p -> p.getUser().getId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Pet not found for this user"));
        UserJpaEntity userRef = userJpaRepository.findById(Math.toIntExact(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

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
