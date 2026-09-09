package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.core.domain.enums.AppContextEnum;

/**
 * Points an app at a pet the user already owns — e.g. "my Wallet dog Rex
 * should answer for Health too" — without creating a new pet or touching its
 * specie/name/health. Replaces whatever pet previously answered for that app.
 */
public interface LinkPetToAppUseCase {
    void execute(String userEmail, Integer petId, AppContextEnum appContext);
}
