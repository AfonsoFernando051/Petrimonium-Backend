package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.core.domain.Pet;
import java.util.List;

public interface ListMyPetsUseCase {
    List<Pet> execute(String userEmail);
}
