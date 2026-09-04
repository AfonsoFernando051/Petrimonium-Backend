package com.jf.PetApp.presentation.auth.dto;

import jakarta.validation.constraints.NotBlank;

// appContext is optional (nullable) — "academy", "wallet" or "health", case-insensitive; see
// AppContextEnum#fromRequestValue for how it's parsed and validated.
public record GoogleLoginRequest(
        @NotBlank(message = "idToken is required") String idToken,
        String appContext
) {

}
