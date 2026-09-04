package com.jf.PetApp.application.mentor.exception;

/**
 * Raised when the Mentor is switched off operationally (see {@code app.mentor.enabled}).
 *
 * <p>Distinct from every other Mentor failure on purpose: this is not the provider being down, a
 * rate limit, or a bad request — it is an operator having deliberately stopped the AI. The client
 * should say so plainly rather than offering a retry that is guaranteed to fail for as long as the
 * switch is off.
 */
public class MentorDisabledException extends RuntimeException {

    public MentorDisabledException() {
        super("The Mentor is currently disabled.");
    }
}
