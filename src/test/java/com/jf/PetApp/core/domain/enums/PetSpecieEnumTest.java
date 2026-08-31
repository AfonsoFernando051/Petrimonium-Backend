package com.jf.PetApp.core.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Bare enum constants with no additional logic -- minimal values()/valueOf() smoke test per
 * this batch's exhaustive-coverage scope.
 */
class PetSpecieEnumTest {

    @Test
    void values_ContainsAllSevenDeclaredConstants() {
        assertEquals(7, PetSpecieEnum.values().length);
    }

    @Test
    void valueOf_WithAKnownName_ReturnsTheMatchingConstant() {
        assertEquals(PetSpecieEnum.DOG, PetSpecieEnum.valueOf("DOG"));
        assertEquals(PetSpecieEnum.CAT, PetSpecieEnum.valueOf("CAT"));
        assertEquals(PetSpecieEnum.WOLF, PetSpecieEnum.valueOf("WOLF"));
        assertEquals(PetSpecieEnum.FOX, PetSpecieEnum.valueOf("FOX"));
        assertEquals(PetSpecieEnum.BEAR, PetSpecieEnum.valueOf("BEAR"));
        assertEquals(PetSpecieEnum.LION, PetSpecieEnum.valueOf("LION"));
        assertEquals(PetSpecieEnum.OWL, PetSpecieEnum.valueOf("OWL"));
    }

    @Test
    void valueOf_WithAnUnknownName_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> PetSpecieEnum.valueOf("DRAGON"));
    }
}
