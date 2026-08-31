package com.jf.PetApp.application.auth.port;

import com.jf.PetApp.core.domain.User;

public interface TokenProvider {
    String generateToken(User user);

    boolean validate(String token);

	String extractSubject(String token);
}
