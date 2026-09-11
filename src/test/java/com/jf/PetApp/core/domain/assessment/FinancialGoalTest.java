package com.jf.PetApp.core.domain.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Plain enum with no custom behavior — confirms its five known constants and
 * standard enum contract.
 */
class FinancialGoalTest {

    @Test
    void values_ContainsAllFiveGoals() {
        assertEquals(5, FinancialGoal.values().length);
    }

    @Test
    void valueOf_KnownName_ReturnsConstant() {
        assertEquals(FinancialGoal.EMERGENCY_FUND, FinancialGoal.valueOf("EMERGENCY_FUND"));
        assertEquals(FinancialGoal.GET_OUT_OF_DEBT, FinancialGoal.valueOf("GET_OUT_OF_DEBT"));
        assertEquals(FinancialGoal.BUY_IMPORTANT_THING, FinancialGoal.valueOf("BUY_IMPORTANT_THING"));
        assertEquals(FinancialGoal.INVEST_WITH_CONFIDENCE, FinancialGoal.valueOf("INVEST_WITH_CONFIDENCE"));
        assertEquals(FinancialGoal.JUST_WANT_TO_LEARN, FinancialGoal.valueOf("JUST_WANT_TO_LEARN"));
    }

    @Test
    void valueOf_UnknownName_Throws() {
        assertThrows(IllegalArgumentException.class, () -> FinancialGoal.valueOf("UNKNOWN"));
    }
}
