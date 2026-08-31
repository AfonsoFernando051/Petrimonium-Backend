package com.jf.PetApp.application.pet.usecase;

import com.jf.PetApp.core.domain.enums.PetSpecieEnum;

public interface ConfigurePetUseCase {
    void execute(String userEmail, PetSpecieEnum specie);
}
