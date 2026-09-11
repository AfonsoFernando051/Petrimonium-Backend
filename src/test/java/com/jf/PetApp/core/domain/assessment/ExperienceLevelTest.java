package com.jf.PetApp.core.domain.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Plain enum with no custom behavior — confirms its three known constants and
 * standard enum contract.
 */
class ExperienceLevelTest {

    @Test
    void values_ContainsAllThreeLevels() {
        assertEquals(3, ExperienceLevel.values().length);
    }

    @Test
    void valueOf_KnownName_ReturnsConstant() {
        assertEquals(ExperienceLevel.NOVICE, ExperienceLevel.valueOf("NOVICE"));
        assertEquals(ExperienceLevel.CURIOUS, ExperienceLevel.valueOf("CURIOUS"));
        assertEquals(ExperienceLevel.PRACTITIONER, ExperienceLevel.valueOf("PRACTITIONER"));
    }

    @Test
    void valueOf_UnknownName_Throws() {
        assertThrows(IllegalArgumentException.class, () -> ExperienceLevel.valueOf("UNKNOWN"));
    }
}
