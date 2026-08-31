package com.jf.PetApp.application.investment.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssetQuoteResponseTest {

    @Test
    void fiveArgConstructor_KeepsExplicitChangePercent() {
        AssetQuoteResponse dto = new AssetQuoteResponse("PETR4", "Petrobras", 35.5, "BRL", -1.25);

        assertEquals("PETR4", dto.symbol());
        assertEquals("Petrobras", dto.shortName());
        assertEquals(35.5, dto.regularMarketPrice());
        assertEquals("BRL", dto.currency());
        assertEquals(-1.25, dto.regularMarketChangePercent());
    }

    @Test
    void fourArgConstructor_DefaultsChangePercentToZero() {
        AssetQuoteResponse dto = new AssetQuoteResponse("PETR4", "Petrobras", 35.5, "BRL");

        assertEquals(0.0, dto.regularMarketChangePercent());
    }
}
