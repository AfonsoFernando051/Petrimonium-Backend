package com.jf.PetApp.application.auth.dto;

import com.jf.PetApp.core.domain.enums.AppContextEnum;

public record LoginCommand(String email, String password, AppContextEnum appContext) {

    public LoginCommand(String email, String password) {
        this(email, password, null);
    }
}
