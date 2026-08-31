package com.jf.PetApp.application.auth.port;

import java.util.Optional;

import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;

public interface TokenProvider {
    /** {@code appContext} may be null — the resulting token simply carries no app_context claim. */
    String generateToken(User user, AppContextEnum appContext);

    boolean validate(String token);

	String extractSubject(String token);

    Optional<AppContextEnum> extractAppContext(String token);
}
