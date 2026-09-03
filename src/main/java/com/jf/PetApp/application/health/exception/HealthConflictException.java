package com.jf.PetApp.application.health.exception;

/** A user-resolvable Health state conflict, exposed as HTTP 409. */
public class HealthConflictException extends RuntimeException {
    private final String code;

    public HealthConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
