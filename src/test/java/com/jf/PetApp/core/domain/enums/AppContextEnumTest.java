package com.jf.PetApp.core.domain.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;

/**
 * Health joins Academy and Wallet as a third app_context. The claim value, the granted authority
 * and the accepted request value all have to line up, because a session's whole app scope — and
 * so which product's data it can read — is decided by these three strings agreeing.
 */
class AppContextEnumTest {

    @ParameterizedTest
    @EnumSource(AppContextEnum.class)
    void aClaimRoundTripsBackToTheSameContext(AppContextEnum context) {
        assertEquals(Optional.of(context), AppContextEnum.fromClaimValue(context.claimValue()));
        assertEquals(Optional.of(context), AppContextEnum.fromAuthority(context.authority()));
    }

    @Test
    void healthHasItsOwnClaimAndAuthority() {
        assertEquals("health", AppContextEnum.HEALTH.claimValue());
        assertEquals("APP_CONTEXT_HEALTH", AppContextEnum.HEALTH.authority());
    }

    @Test
    void theAppsRequestValueIsAcceptedCaseInsensitively() {
        assertEquals(AppContextEnum.HEALTH, AppContextEnum.fromRequestValue("health"));
        assertEquals(AppContextEnum.HEALTH, AppContextEnum.fromRequestValue(" Health "));
        assertEquals(AppContextEnum.WALLET, AppContextEnum.fromRequestValue("wallet"));
        assertEquals(AppContextEnum.ACADEMY, AppContextEnum.fromRequestValue("academy"));
    }

    @Test
    void anAbsentRequestValueStillMeansAnUnscopedSession() {
        assertNull(AppContextEnum.fromRequestValue(null));
        assertNull(AppContextEnum.fromRequestValue("  "));
    }

    @Test
    void aTypedRequestValueFailsLoudlyRatherThanMintingAnUnscopedToken() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AppContextEnum.fromRequestValue("healht"));

        assertTrue(e.getMessage().contains("health"), "the message must list the values the apps may send");
    }

    @Test
    void oneContextsAuthorityNeverResolvesToAnother() {
        assertEquals(Optional.empty(), AppContextEnum.fromAuthority("APP_CONTEXT_"));
        assertEquals(Optional.empty(), AppContextEnum.fromAuthority("ROLE_USER"));
        assertEquals(Optional.empty(), AppContextEnum.fromClaimValue("banking"));
        assertEquals(Optional.empty(), AppContextEnum.fromClaimValue(null));
    }
}
