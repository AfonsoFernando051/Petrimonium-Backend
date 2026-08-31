package com.jf.PetApp.application.auth.usecase;

import com.jf.PetApp.application.auth.exception.PasswordResetTokenInvalidException;

public interface ResetPasswordUseCase {

    void execute(String rawToken, String newPassword) throws PasswordResetTokenInvalidException;
}
