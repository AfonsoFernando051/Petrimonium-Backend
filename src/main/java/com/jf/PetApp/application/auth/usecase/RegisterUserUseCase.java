package com.jf.PetApp.application.auth.usecase;

import com.jf.PetApp.application.auth.dto.RegisterCommand;
import com.jf.PetApp.application.auth.dto.RegisterResult;

public interface RegisterUserUseCase {
	RegisterResult execute(RegisterCommand command);
}
