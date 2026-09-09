package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import java.util.Optional;

public interface GetMyPetUseCase {
    Optional<Pet> execute(String userEmail, AppContextEnum appContext);
}
