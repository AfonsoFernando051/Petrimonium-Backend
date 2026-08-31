package com.jf.PetApp.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Finance is a plain getter/setter holder with no custom behavior — this just
 * confirms the accessors round-trip correctly.
 */
class FinanceTest {

    @Test
    void settersAndGetters_RoundTripEveryField() {
        Finance finance = new Finance();
        Investment investment = new Investment(1, "user@test.com", "PETR4", 10.0, 20.0, null, null);

        finance.setId(7);
        finance.setBalance(BigDecimal.valueOf(1234.56));
        finance.setInvestments(List.of(investment));

        assertEquals(7, finance.getId());
        assertEquals(BigDecimal.valueOf(1234.56), finance.getBalance());
        assertTrue(finance.getInvestments().contains(investment));
    }
}
