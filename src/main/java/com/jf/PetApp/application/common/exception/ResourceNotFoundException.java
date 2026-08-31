package com.jf.PetApp.application.common.exception;

/**
 * "Authenticated, but the resource/user this request refers to no longer exists" — distinct
 * from a malformed/invalid request ({@link IllegalArgumentException}, mapped to 400). Shared
 * across features (gamification, learning, investment, mentor, onboarding all throw it for the
 * same "user not found" shape) rather than duplicated per-feature, since the semantics and HTTP
 * mapping are identical everywhere it's used.
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
