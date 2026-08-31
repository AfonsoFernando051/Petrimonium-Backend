package com.jf.PetApp.core.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bare enum constants with no additional logic (no label/category mapping method) -- minimal
 * values()/valueOf() smoke test per this batch's exhaustive-coverage scope.
 */
class InvestmentTypeTest {

    @Test
    void values_ContainsAllSixDeclaredConstants() {
        InvestmentType[] values = InvestmentType.values();

        assertEquals(6, values.length);
        assertTrue(java.util.List.of(values).containsAll(java.util.List.of(
                InvestmentType.STOCKS, InvestmentType.FIXED_INCOME, InvestmentType.REAL_ESTATE,
                InvestmentType.CRYPTO, InvestmentType.FUNDS, InvestmentType.OTHERS)));
    }

    @Test
    void valueOf_WithAKnownName_ReturnsTheMatchingConstant() {
        assertEquals(InvestmentType.STOCKS, InvestmentType.valueOf("STOCKS"));
    }

    @Test
    void valueOf_WithAnUnknownName_ThrowsIllegalArgumentException() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> InvestmentType.valueOf("NOT_A_TYPE"));
    }
}
