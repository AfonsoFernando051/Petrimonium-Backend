package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.core.domain.enums.AppContextEnum;

public interface GetPetStatusUseCase {
    boolean execute(String userEmail, AppContextEnum appContext);
}
