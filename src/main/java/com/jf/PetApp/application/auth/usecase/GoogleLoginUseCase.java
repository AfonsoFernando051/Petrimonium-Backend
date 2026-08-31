package com.jf.PetApp.application.auth.usecase;

import com.jf.PetApp.application.auth.dto.GoogleLoginCommand;
import com.jf.PetApp.application.auth.dto.LoginResult;
import com.jf.PetApp.application.auth.exception.AuthenticationException;

public interface GoogleLoginUseCase {

    LoginResult execute(GoogleLoginCommand command) throws AuthenticationException;
}
