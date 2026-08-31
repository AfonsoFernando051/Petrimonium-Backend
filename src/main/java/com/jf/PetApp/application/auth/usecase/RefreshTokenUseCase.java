package com.jf.PetApp.application.auth.usecase;

import com.jf.PetApp.application.auth.dto.RefreshTokenCommand;
import com.jf.PetApp.application.auth.dto.RefreshTokenResult;
import com.jf.PetApp.application.auth.exception.AuthenticationException;

public interface RefreshTokenUseCase {
    RefreshTokenResult execute(RefreshTokenCommand command) throws AuthenticationException;
}
