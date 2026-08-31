package com.jf.PetApp.core.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Bare enum constants with no additional logic -- minimal values()/valueOf() smoke test per
 * this batch's exhaustive-coverage scope.
 */
class RoleEnumTest {

    @Test
    void values_ContainsBothDeclaredConstants() {
        assertEquals(2, RoleEnum.values().length);
    }

    @Test
    void valueOf_WithAKnownName_ReturnsTheMatchingConstant() {
        assertEquals(RoleEnum.ADMIN, RoleEnum.valueOf("ADMIN"));
        assertEquals(RoleEnum.USER, RoleEnum.valueOf("USER"));
    }

    @Test
    void valueOf_WithAnUnknownName_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> RoleEnum.valueOf("SUPERADMIN"));
    }
}
