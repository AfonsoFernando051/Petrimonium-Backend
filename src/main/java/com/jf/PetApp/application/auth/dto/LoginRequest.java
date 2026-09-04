package com.jf.PetApp.application.auth.dto;

import jakarta.validation.constraints.NotBlank;

// appContext is optional (nullable) — "academy", "wallet" or "health", case-insensitive; see
// AppContextEnum#fromRequestValue for how it's parsed and validated.
public record LoginRequest(@NotBlank String email, @NotBlank String password, String appContext) {

    public LoginRequest(String email, String password) {
        this(email, password, null);
    }
}
