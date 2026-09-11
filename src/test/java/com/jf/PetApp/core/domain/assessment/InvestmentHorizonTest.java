package com.jf.PetApp.core.domain.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Plain enum with no custom behavior — confirms its four known constants and
 * standard enum contract.
 */
class InvestmentHorizonTest {

    @Test
    void values_ContainsAllFourHorizons() {
        assertEquals(4, InvestmentHorizon.values().length);
    }

    @Test
    void valueOf_KnownName_ReturnsConstant() {
        assertEquals(InvestmentHorizon.UP_TO_ONE_YEAR, InvestmentHorizon.valueOf("UP_TO_ONE_YEAR"));
        assertEquals(InvestmentHorizon.ONE_TO_FIVE_YEARS, InvestmentHorizon.valueOf("ONE_TO_FIVE_YEARS"));
        assertEquals(InvestmentHorizon.MORE_THAN_FIVE_YEARS, InvestmentHorizon.valueOf("MORE_THAN_FIVE_YEARS"));
        assertEquals(InvestmentHorizon.NOT_SURE_YET, InvestmentHorizon.valueOf("NOT_SURE_YET"));
    }

    @Test
    void valueOf_UnknownName_Throws() {
        assertThrows(IllegalArgumentException.class, () -> InvestmentHorizon.valueOf("UNKNOWN"));
    }
}
