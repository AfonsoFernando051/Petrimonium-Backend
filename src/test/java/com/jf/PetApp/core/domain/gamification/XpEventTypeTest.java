package com.jf.PetApp.core.domain.gamification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Bare enum constants with no additional logic -- minimal values()/valueOf() smoke test per
 * this batch's exhaustive-coverage scope.
 */
class XpEventTypeTest {

    @Test
    void values_ContainsAllDeclaredConstants() {
        assertEquals(3, XpEventType.values().length);
    }

    @Test
    void valueOf_WithAKnownName_ReturnsTheMatchingConstant() {
        assertEquals(XpEventType.LESSON_COMPLETED, XpEventType.valueOf("LESSON_COMPLETED"));
        assertEquals(XpEventType.MODULE_COMPLETED, XpEventType.valueOf("MODULE_COMPLETED"));
        assertEquals(XpEventType.SIMULATOR_COMPLETED, XpEventType.valueOf("SIMULATOR_COMPLETED"));
    }

    @Test
    void valueOf_WithAnUnknownName_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> XpEventType.valueOf("STREAK_KEPT"));
    }
}
