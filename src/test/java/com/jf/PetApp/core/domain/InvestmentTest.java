package com.jf.PetApp.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.jf.PetApp.core.domain.enums.InvestmentType;

/**
 * Investment is a plain record with no custom behavior beyond the
 * compiler-generated accessors/equals — this confirms construction and the
 * documented "null id means unsaved" convention.
 */
class InvestmentTest {

    @Test
    void accessors_ReturnConstructedValues() {
        LocalDate purchaseDate = LocalDate.of(2026, 1, 15);
        Investment investment = new Investment(
                1, "user@test.com", "PETR4", 10.0, 25.5, purchaseDate, InvestmentType.STOCKS);

        assertEquals(1, investment.id());
        assertEquals("user@test.com", investment.userEmail());
        assertEquals("PETR4", investment.name());
        assertEquals(10.0, investment.quantity());
        assertEquals(25.5, investment.purchasePrice());
        assertEquals(purchaseDate, investment.purchaseDate());
        assertEquals(InvestmentType.STOCKS, investment.type());
    }

    @Test
    void nullId_RepresentsAnUnsavedLot() {
        Investment investment = new Investment(
                null, "user@test.com", "PETR4", 10.0, 25.5, LocalDate.now(), InvestmentType.STOCKS);

        assertNull(investment.id());
    }

    @Test
    void equals_SameValues_AreEqual() {
        LocalDate date = LocalDate.of(2026, 1, 15);
        Investment a = new Investment(1, "user@test.com", "PETR4", 10.0, 25.5, date, InvestmentType.STOCKS);
        Investment b = new Investment(1, "user@test.com", "PETR4", 10.0, 25.5, date, InvestmentType.STOCKS);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_DifferentValues_AreNotEqual() {
        LocalDate date = LocalDate.of(2026, 1, 15);
        Investment a = new Investment(1, "user@test.com", "PETR4", 10.0, 25.5, date, InvestmentType.STOCKS);
        Investment b = new Investment(2, "user@test.com", "PETR4", 10.0, 25.5, date, InvestmentType.STOCKS);

        assertNotEquals(a, b);
    }
}
