package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.DividendType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetDetailsResponseDTOTest {

    private UserPositionDTO samplePosition() {
        return new UserPositionDTO(
                BigDecimal.valueOf(10), BigDecimal.valueOf(20), BigDecimal.valueOf(200), BigDecimal.valueOf(250),
                BigDecimal.valueOf(50), BigDecimal.valueOf(25), BigDecimal.valueOf(0.1));
    }

    private DividendRadarEntryDTO sampleDividend() {
        return new DividendRadarEntryDTO(
                "PETR4", DividendType.DIVIDENDO, "Dividendo", 1.5,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), LocalDate.of(2025, 12, 1),
                10.0, 15.0, DividendRadarEntryDTO.STATUS_PAID);
    }

    @Test
    void builder_BuildsDTOWithAllFieldsSet() {
        UserPositionDTO position = samplePosition();
        List<DividendRadarEntryDTO> dividends = List.of(sampleDividend());

        AssetDetailsResponseDTO dto = AssetDetailsResponseDTO.builder()
                .ticker("PETR4")
                .shortName("Petrobras PN")
                .longName("Petroleo Brasileiro S.A.")
                .assetType("stock")
                .sector("Energy")
                .industry("Oil & Gas")
                .logoUrl("https://example.com/logo.png")
                .currentPrice(35.5)
                .previousClose(34.0)
                .dailyChange(1.5)
                .dailyChangePercent(4.4)
                .currency("BRL")
                .marketCap(500000.0)
                .priceToEarnings(8.0)
                .priceToBook(1.2)
                .evToEbitda(4.5)
                .dividendYield(0.12)
                .returnOnEquity(0.2)
                .returnOnAssets(0.1)
                .netMargin(0.15)
                .operatingMargin(0.18)
                .netDebt(1000.0)
                .debtToEquity(0.5)
                .totalCash(2000.0)
                .totalRevenue(30000.0)
                .ebitda(6000.0)
                .netAssetValue(10.0)
                .pvp(1.1)
                .fiftyTwoWeekHigh(40.0)
                .fiftyTwoWeekLow(28.0)
                .averageVolume(100000L)
                .regularMarketVolume(120000L)
                .userPosition(position)
                .recentDividends(dividends)
                .dataSource("brapi")
                .lastUpdated("2026-08-23T00:00:00Z")
                .dataStatus("FRESH")
                .build();

        assertEquals("PETR4", dto.ticker());
        assertEquals("Petrobras PN", dto.shortName());
        assertEquals("stock", dto.assetType());
        assertEquals(35.5, dto.currentPrice());
        assertEquals("BRL", dto.currency());
        assertEquals(500000.0, dto.marketCap());
        assertSame(position, dto.userPosition());
        assertEquals(dividends, dto.recentDividends());
        assertEquals("brapi", dto.dataSource());
        assertEquals("FRESH", dto.dataStatus());
    }

    @Test
    void builder_DefaultsRecentDividendsToEmptyListWhenNeverSet() {
        AssetDetailsResponseDTO dto = AssetDetailsResponseDTO.builder()
                .ticker("PETR4")
                .build();

        assertNotNull(dto.recentDividends());
        assertTrue(dto.recentDividends().isEmpty());
    }

    @Test
    void builder_RecentDividendsAcceptsNullAsEmptyList() {
        AssetDetailsResponseDTO dto = AssetDetailsResponseDTO.builder()
                .ticker("PETR4")
                .recentDividends(null)
                .build();

        assertNotNull(dto.recentDividends());
        assertTrue(dto.recentDividends().isEmpty());
    }

    @Test
    void toBuilder_CopiesEveryFieldFromTheBase() {
        AssetDetailsResponseDTO base = AssetDetailsResponseDTO.builder()
                .ticker("PETR4")
                .shortName("Petrobras PN")
                .currentPrice(35.5)
                .userPosition(samplePosition())
                .recentDividends(List.of(sampleDividend()))
                .dataStatus("FRESH")
                .build();

        AssetDetailsResponseDTO copy = base.toBuilder()
                .dataStatus("CACHED")
                .build();

        assertEquals(base.ticker(), copy.ticker());
        assertEquals(base.shortName(), copy.shortName());
        assertEquals(base.currentPrice(), copy.currentPrice());
        assertEquals(base.userPosition(), copy.userPosition());
        assertEquals(base.recentDividends(), copy.recentDividends());
        // The whole point of toBuilder(): the base is untouched, only the new builder call changed.
        assertEquals("FRESH", base.dataStatus());
        assertEquals("CACHED", copy.dataStatus());
    }
}
