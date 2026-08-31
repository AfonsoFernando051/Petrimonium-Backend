package com.jf.PetApp.application.auth.exception;

/**
 * Covers "token doesn't exist," "already used," and "expired" as a single outcome — deliberately
 * indistinguishable to the caller, same enumeration-avoidance spirit as
 * {@code /auth/forgot-password} always returning a generic 200.
 */
public class PasswordResetTokenInvalidException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PasswordResetTokenInvalidException() {
        super("Password reset token is invalid, expired, or already used");
    }
}
