package com.jf.PetApp.application.auth.dto;

import com.jf.PetApp.core.domain.enums.AppContextEnum;

public record GoogleLoginCommand(String idToken, AppContextEnum appContext) {

    public GoogleLoginCommand(String idToken) {
        this(idToken, null);
    }
}
