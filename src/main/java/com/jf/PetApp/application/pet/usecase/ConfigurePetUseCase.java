package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.core.domain.enums.AppContextEnum;
import com.jf.PetApp.core.domain.enums.PetSpecieEnum;

public interface ConfigurePetUseCase {
    /**
     * @param appContext which app's pet to create/update — each user has one pet per app.
     * @param name the name chosen during onboarding, applied only when a new pet is created.
     *             Blank or null falls back to the generated "{@code <SPECIE> Companion}" default.
     */
    void execute(String userEmail, AppContextEnum appContext, PetSpecieEnum specie, String name);
}
