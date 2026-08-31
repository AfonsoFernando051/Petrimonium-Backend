package com.jf.PetApp.core.domain.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * InvestorProfile is a plain enum with no custom behavior — this confirms
 * its three known constants and standard enum contract.
 */
class InvestorProfileTest {

    @Test
    void values_ContainsAllThreeProfiles() {
        assertEquals(3, InvestorProfile.values().length);
    }

    @Test
    void valueOf_KnownName_ReturnsConstant() {
        assertEquals(InvestorProfile.GUARDIAN, InvestorProfile.valueOf("GUARDIAN"));
        assertEquals(InvestorProfile.TACTICIAN, InvestorProfile.valueOf("TACTICIAN"));
        assertEquals(InvestorProfile.ADVENTURER, InvestorProfile.valueOf("ADVENTURER"));
    }

    @Test
    void valueOf_UnknownName_Throws() {
        assertThrows(IllegalArgumentException.class, () -> InvestorProfile.valueOf("UNKNOWN"));
    }
}
