package com.jf.PetApp.core.domain.enums;

import java.util.Optional;

/**
 * Which Flutter app a session belongs to (docs/ECOSYSTEM.md, CROSS_REPO_CONTRACTS.md §1). Not an
 * authorization role — {@link RoleEnum} still governs ADMIN/USER — this is what lets the BFF tell
 * a Wallet session apart from an Academy one so real_portfolio never answers an Academy request
 * and vice versa.
 */
public enum AppContextEnum {
    ACADEMY,
    WALLET;

    /** Lowercase JWT claim value, per the {@code app_context} shape already published to the
     * Academy/Wallet repos in CROSS_REPO_CONTRACTS.md. */
    public String claimValue() {
        return name().toLowerCase();
    }

    /** Spring Security authority granted for a session carrying this context. */
    public String authority() {
        return "APP_CONTEXT_" + name();
    }

    /**
     * Parses a claim out of a JWT this backend itself signed. An unrecognized value is treated
     * as absent rather than rejected — the token is still valid, it just carries no context, the
     * same as an older token minted before this claim existed.
     */
    public static Optional<AppContextEnum> fromClaimValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Parses the client-supplied {@code appContext} request field. Absent/blank means "not
     * specified" (allowed — the resulting token simply carries no context claim); anything else
     * must be a real value, since a typo here should fail loudly rather than silently mint an
     * unscoped token.
     */
    public static AppContextEnum fromRequestValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("appContext must be 'academy' or 'wallet'");
        }
    }
}
