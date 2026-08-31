package com.jf.PetApp.application.investment.service;

import com.jf.PetApp.application.investment.dto.AssetDetailsResponseDTO;
import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.dto.DividendRadarEntryDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AssetDetailsResponseMapperTest {

    private final AssetDetailsResponseMapper mapper = new AssetDetailsResponseMapper();

    @Test
    void fromEnrichedData_MapsKnownFieldsAndLeavesUnknownOnesNull() {
        Map<String, Object> data = Map.of(
            "shortName", "PETROBRAS",
            "regularMarketPrice", 35.5,
            "regularMarketVolume", 1_000_000L
        );

        AssetDetailsResponseDTO result = mapper.fromEnrichedData("PETR4", data, null, List.of());

        assertEquals("PETR4", result.ticker());
        assertEquals("PETROBRAS", result.shortName());
        assertEquals(35.5, result.currentPrice());
        assertEquals(1_000_000L, result.regularMarketVolume());
        assertNull(result.sector()); // provider didn't return it — never fabricated
        assertEquals("FRESH", result.dataStatus());
    }

    @Test
    void fromEnrichedData_PrefersTopLevelSectorOverSummaryProfile() {
        Map<String, Object> data = Map.of(
            "sector", "Energy",
            "summaryProfile", Map.of("sector", "Should not be used", "industry", "Oil & Gas")
        );

        AssetDetailsResponseDTO result = mapper.fromEnrichedData("PETR4", data, null, List.of());

        assertEquals("Energy", result.sector());
        assertEquals("Oil & Gas", result.industry()); // falls back to summaryProfile when absent at top level
    }

    @Test
    void fromEnrichedData_DetectsFiiFromTickerSuffix() {
        AssetDetailsResponseDTO result = mapper.fromEnrichedData("HGLG11", Map.of(), null, List.of());

        assertEquals("fii", result.assetType());
    }

    @Test
    void fromEnrichedData_DetectsBdrFromTickerSuffix() {
        AssetDetailsResponseDTO result = mapper.fromEnrichedData("AAPL34", Map.of(), null, List.of());

        assertEquals("bdr", result.assetType());
    }

    @Test
    void fromSimpleQuote_MapsBasicFieldsOnlyAndStatusIsPartial() {
        AssetQuoteResponse quote = new AssetQuoteResponse("PETR4", "PETROBRAS", 35.5, "BRL");

        AssetDetailsResponseDTO result = mapper.fromSimpleQuote("PETR4", quote, null, List.of());

        assertEquals(35.5, result.currentPrice());
        assertEquals("BRL", result.currency());
        assertEquals("PARTIAL", result.dataStatus());
        assertNull(result.marketCap());
    }

    @Test
    void unavailable_EveryMarketFieldIsNull() {
        AssetDetailsResponseDTO result = mapper.unavailable("PETR4", null);

        assertEquals("UNAVAILABLE", result.dataStatus());
        assertNull(result.currentPrice());
        assertEquals(List.of(), result.recentDividends());
    }

    @Test
    void withUserPositionAndStatus_KeepsMarketDataButSwapsPositionAndStatus() {
        AssetDetailsResponseDTO base = mapper.fromEnrichedData(
            "PETR4", Map.of("regularMarketPrice", 35.5), null, List.of()
        );

        AssetDetailsResponseDTO result = mapper.withUserPositionAndStatus(base, null, "CACHED");

        assertEquals(35.5, result.currentPrice());
        assertEquals("CACHED", result.dataStatus());
    }

    @Test
    void fromEnrichedData_DetectsAssetTypeFromExplicitProviderType() {
        Map<String, Object> data = Map.of("type", "fund");

        AssetDetailsResponseDTO result = mapper.fromEnrichedData("XPML11", data, null, List.of());

        assertEquals("fii", result.assetType());
    }

    @Test
    void fromEnrichedData_PassesThroughRecentDividendsUnchanged() {
        List<DividendRadarEntryDTO> dividends = List.of();

        AssetDetailsResponseDTO result = mapper.fromEnrichedData("PETR4", Map.of(), null, dividends);

        assertEquals(dividends, result.recentDividends());
    }
}
