package com.jf.PetApp.application.settings.dto;

/**
 * A request to erase the account identified by {@code email}, together with the credential the
 * caller is re-proving themselves with.
 *
 * <p>Both credentials are nullable and exactly one is expected to be present, matching however
 * the account signs in: {@code password} for an account that has a local one,
 * {@code googleIdToken} for an account created through Google (which has no password at all —
 * see {@code User.createFromGoogle}). An account linked to both can use either.
 *
 * <p>The session's bearer token is deliberately <em>not</em> sufficient on its own here. It is
 * the only thing this endpoint used to ask for, which put the single irreversible, no-grace-
 * period operation in the product one stolen access token away — a lower bar than replacing a
 * portfolio, which already demands an explicit confirmation flag.
 */
public record DeleteAccountCommand(String email, String password, String googleIdToken) {
}
