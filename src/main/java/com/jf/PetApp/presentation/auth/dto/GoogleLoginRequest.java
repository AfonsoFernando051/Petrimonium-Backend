package com.jf.PetApp.presentation.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "idToken is required") String idToken
) {

}
