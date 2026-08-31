package com.jf.PetApp.application.auth.port;

/**
 * Delivers the raw, one-time password-reset token to the user out of band (email today).
 * Kept separate from {@link PasswordResetTokenRepositoryPort} because persistence and
 * notification are different concerns with different failure modes — a mailer outage should
 * never be confused with a storage failure.
 */
public interface PasswordResetMailerPort {

    void sendPasswordResetEmail(String toEmail, String rawToken);
}
