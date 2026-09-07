package com.jf.PetApp.application.settings.usecase;

public interface DeleteAccountUseCase {
    /** Apaga definitivamente a conta e todos os seus dados. Irreversível. */
    void execute(String email);
}
