package com.jf.PetApp.application.auth.usecase;

import com.jf.PetApp.application.auth.dto.LogoutCommand;

public interface LogoutUseCase {
    void execute(LogoutCommand command);
}
