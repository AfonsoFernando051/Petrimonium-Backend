package com.jf.PetApp.infrastructure.external;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * There is no legitimate B3 integration/contract/credential in this
 * codebase — these tests exist to lock in that this adapter can never
 * silently start reporting itself as enabled from a partial config, since
 * {@code isEnabled()} is the only thing standing between "safe, disabled by
 * default" and accidentally trying to sync against a provider with no real
 * implementation behind it.
 */
class B3RealPortfolioSyncAdapterTest {

    private B3RealPortfolioSyncAdapter adapter() {
        return new B3RealPortfolioSyncAdapter();
    }

    @Test
    void isEnabled_WithNeitherFlagNorTokenSet_IsFalse() {
        assertFalse(adapter().isEnabled());
    }

    @Test
    void isEnabled_WithFlagTrueButBlankToken_IsFalse() {
        B3RealPortfolioSyncAdapter adapter = adapter();
        ReflectionTestUtils.setField(adapter, "syncEnabled", true);
        ReflectionTestUtils.setField(adapter, "token", "");

        assertFalse(adapter.isEnabled());
    }

    @Test
    void isEnabled_WithTokenSetButFlagFalse_IsFalse() {
        B3RealPortfolioSyncAdapter adapter = adapter();
        ReflectionTestUtils.setField(adapter, "syncEnabled", false);
        ReflectionTestUtils.setField(adapter, "token", "a-real-looking-token");

        assertFalse(adapter.isEnabled());
    }

    @Test
    void isEnabled_WithBothFlagAndTokenSet_IsTrue() {
        B3RealPortfolioSyncAdapter adapter = adapter();
        ReflectionTestUtils.setField(adapter, "syncEnabled", true);
        ReflectionTestUtils.setField(adapter, "token", "a-real-looking-token");

        assertTrue(adapter.isEnabled());
    }

    @Test
    void fetchPositions_WhileDisabled_ThrowsRatherThanFabricatingData() {
        assertThrows(IllegalStateException.class, () -> adapter().fetchPositions("any-account"));
    }

    @Test
    void providerName_IsB3() {
        assertTrue(adapter().providerName().equals("B3"));
    }
}
