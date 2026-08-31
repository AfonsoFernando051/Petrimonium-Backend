package com.jf.PetApp.application.auth.usecase;

public interface RequestPasswordResetUseCase {

    /**
     * Never reveals whether {@code email} belongs to a real account — the caller
     * ({@code AuthController}) always returns the same generic response either way.
     */
    void execute(String email);
}
