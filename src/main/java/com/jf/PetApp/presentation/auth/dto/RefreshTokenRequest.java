package com.jf.PetApp.presentation.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Shared request shape for both /auth/refresh and /auth/logout — both just need the raw refresh token. */
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required") String refreshToken
) {
}
