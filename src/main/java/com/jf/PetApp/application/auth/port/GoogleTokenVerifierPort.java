package com.jf.PetApp.application.auth.port;

import com.jf.PetApp.application.auth.exception.AuthenticationException;

public interface GoogleTokenVerifierPort {

    /**
     * Verifies a Google Sign-In ID token and returns the identity it carries.
     *
     * @throws AuthenticationException if the token is invalid, expired, issued for a
     *         different audience, or its email is not verified.
     */
    GoogleUserInfo verify(String idToken) throws AuthenticationException;
}
