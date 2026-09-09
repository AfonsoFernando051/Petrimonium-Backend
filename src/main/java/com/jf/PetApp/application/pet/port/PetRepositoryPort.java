package com.jf.PetApp.application.pet.port;

import java.util.List;
import java.util.Optional;

import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.enums.AppContextEnum;

public interface PetRepositoryPort {

    Optional<Pet> findByUserIdAndAppContext(Long userId, AppContextEnum appContext);

    List<Pet> findAllByUserId(Long userId);

    /** Creates or updates {@code pet} and points {@code appContext} at it, replacing whatever pet answered for that app before. */
    Pet saveAndLink(Pet pet, AppContextEnum appContext);

    /** Points {@code appContext} at an existing pet the user already owns, without touching its specie/name/health. */
    void link(Integer petId, Long userId, AppContextEnum appContext);

    void deleteAllByUserId(Long userId);
}
