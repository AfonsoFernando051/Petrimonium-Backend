package com.jf.PetApp.application.investment.dto;

import java.util.List;

/**
 * Rich asset details response combining market data from the financial
 * data provider with the authenticated user's position. Every field is
 * nullable — the UI must handle absent data gracefully (show "Data
 * unavailable") rather than expecting every field to be populated.
 *
 * <p>This DTO intentionally does NOT invent, estimate or hallucinate any
 * financial data. A {@code null} field means "the provider did not return
 * this value" — not "zero" or "not applicable".</p>
 */
public record AssetDetailsResponseDTO(
    // ── Identity ──────────────────────────────────────────────
    String ticker,
    String shortName,
    String longName,
    String assetType,            // "stock", "fii", "etf", "bdr", "unknown"
    String sector,
    String industry,
    String logoUrl,

    // ── Price ─────────────────────────────────────────────────
    Double currentPrice,
    Double previousClose,
    Double dailyChange,
    Double dailyChangePercent,
    String currency,

    // ── Valuation (stocks & FIIs) ─────────────────────────────
    Double marketCap,
    Double priceToEarnings,      // P/E (P/L)
    Double priceToBook,          // P/VP
    Double evToEbitda,           // EV/EBITDA
    Double dividendYield,

    // ── Profitability (stocks) ────────────────────────────────
    Double returnOnEquity,       // ROE
    Double returnOnAssets,       // ROA
    Double netMargin,
    Double operatingMargin,

    // ── Financial health ──────────────────────────────────────
    Double netDebt,
    Double debtToEquity,
    Double totalCash,
    Double totalRevenue,
    Double ebitda,

    // ── FII-specific ──────────────────────────────────────────
    Double netAssetValue,        // Valor patrimonial por cota
    Double pvp,                  // P/VP (often returned separately for FIIs)

    // ── 52-week range ─────────────────────────────────────────
    Double fiftyTwoWeekHigh,
    Double fiftyTwoWeekLow,

    // ── Volume ────────────────────────────────────────────────
    Long averageVolume,
    Long regularMarketVolume,

    // ── User position (null if user doesn't own this asset) ──
    UserPositionDTO userPosition,

    // ── Dividends (most recent, capped) ──────────────────────
    List<DividendRadarEntryDTO> recentDividends,

    // ── Meta ──────────────────────────────────────────────────
    String dataSource,
    String lastUpdated,
    String dataStatus            // "FRESH", "CACHED", "DELAYED", "PARTIAL"
) {

    public static Builder builder() {
        return new Builder();
    }

    /** Returns a builder pre-populated with every field of {@code base} — for the common
     * "same data, different status/position" case ({@code AssetDetailsResponseMapper}'s cache
     * and fallback paths) without repeating all ~40 fields at each call site. */
    public Builder toBuilder() {
        return new Builder()
            .ticker(ticker).shortName(shortName).longName(longName).assetType(assetType)
            .sector(sector).industry(industry).logoUrl(logoUrl)
            .currentPrice(currentPrice).previousClose(previousClose).dailyChange(dailyChange)
            .dailyChangePercent(dailyChangePercent).currency(currency)
            .marketCap(marketCap).priceToEarnings(priceToEarnings).priceToBook(priceToBook)
            .evToEbitda(evToEbitda).dividendYield(dividendYield)
            .returnOnEquity(returnOnEquity).returnOnAssets(returnOnAssets).netMargin(netMargin)
            .operatingMargin(operatingMargin)
            .netDebt(netDebt).debtToEquity(debtToEquity).totalCash(totalCash)
            .totalRevenue(totalRevenue).ebitda(ebitda)
            .netAssetValue(netAssetValue).pvp(pvp)
            .fiftyTwoWeekHigh(fiftyTwoWeekHigh).fiftyTwoWeekLow(fiftyTwoWeekLow)
            .averageVolume(averageVolume).regularMarketVolume(regularMarketVolume)
            .userPosition(userPosition).recentDividends(recentDividends)
            .dataSource(dataSource).lastUpdated(lastUpdated).dataStatus(dataStatus);
    }

    /** Fluent alternative to the 40-argument positional constructor above — every field defaults
     * to {@code null}/empty, so a call site only has to set what it actually knows. */
    public static final class Builder {
        private String ticker;
        private String shortName;
        private String longName;
        private String assetType;
        private String sector;
        private String industry;
        private String logoUrl;
        private Double currentPrice;
        private Double previousClose;
        private Double dailyChange;
        private Double dailyChangePercent;
        private String currency;
        private Double marketCap;
        private Double priceToEarnings;
        private Double priceToBook;
        private Double evToEbitda;
        private Double dividendYield;
        private Double returnOnEquity;
        private Double returnOnAssets;
        private Double netMargin;
        private Double operatingMargin;
        private Double netDebt;
        private Double debtToEquity;
        private Double totalCash;
        private Double totalRevenue;
        private Double ebitda;
        private Double netAssetValue;
        private Double pvp;
        private Double fiftyTwoWeekHigh;
        private Double fiftyTwoWeekLow;
        private Long averageVolume;
        private Long regularMarketVolume;
        private UserPositionDTO userPosition;
        private List<DividendRadarEntryDTO> recentDividends = List.of();
        private String dataSource;
        private String lastUpdated;
        private String dataStatus;

        public Builder ticker(String v) { this.ticker = v; return this; }
        public Builder shortName(String v) { this.shortName = v; return this; }
        public Builder longName(String v) { this.longName = v; return this; }
        public Builder assetType(String v) { this.assetType = v; return this; }
        public Builder sector(String v) { this.sector = v; return this; }
        public Builder industry(String v) { this.industry = v; return this; }
        public Builder logoUrl(String v) { this.logoUrl = v; return this; }
        public Builder currentPrice(Double v) { this.currentPrice = v; return this; }
        public Builder previousClose(Double v) { this.previousClose = v; return this; }
        public Builder dailyChange(Double v) { this.dailyChange = v; return this; }
        public Builder dailyChangePercent(Double v) { this.dailyChangePercent = v; return this; }
        public Builder currency(String v) { this.currency = v; return this; }
        public Builder marketCap(Double v) { this.marketCap = v; return this; }
        public Builder priceToEarnings(Double v) { this.priceToEarnings = v; return this; }
        public Builder priceToBook(Double v) { this.priceToBook = v; return this; }
        public Builder evToEbitda(Double v) { this.evToEbitda = v; return this; }
        public Builder dividendYield(Double v) { this.dividendYield = v; return this; }
        public Builder returnOnEquity(Double v) { this.returnOnEquity = v; return this; }
        public Builder returnOnAssets(Double v) { this.returnOnAssets = v; return this; }
        public Builder netMargin(Double v) { this.netMargin = v; return this; }
        public Builder operatingMargin(Double v) { this.operatingMargin = v; return this; }
        public Builder netDebt(Double v) { this.netDebt = v; return this; }
        public Builder debtToEquity(Double v) { this.debtToEquity = v; return this; }
        public Builder totalCash(Double v) { this.totalCash = v; return this; }
        public Builder totalRevenue(Double v) { this.totalRevenue = v; return this; }
        public Builder ebitda(Double v) { this.ebitda = v; return this; }
        public Builder netAssetValue(Double v) { this.netAssetValue = v; return this; }
        public Builder pvp(Double v) { this.pvp = v; return this; }
        public Builder fiftyTwoWeekHigh(Double v) { this.fiftyTwoWeekHigh = v; return this; }
        public Builder fiftyTwoWeekLow(Double v) { this.fiftyTwoWeekLow = v; return this; }
        public Builder averageVolume(Long v) { this.averageVolume = v; return this; }
        public Builder regularMarketVolume(Long v) { this.regularMarketVolume = v; return this; }
        public Builder userPosition(UserPositionDTO v) { this.userPosition = v; return this; }
        public Builder recentDividends(List<DividendRadarEntryDTO> v) { this.recentDividends = v != null ? v : List.of(); return this; }
        public Builder dataSource(String v) { this.dataSource = v; return this; }
        public Builder lastUpdated(String v) { this.lastUpdated = v; return this; }
        public Builder dataStatus(String v) { this.dataStatus = v; return this; }

        public AssetDetailsResponseDTO build() {
            return new AssetDetailsResponseDTO(
                ticker, shortName, longName, assetType, sector, industry, logoUrl,
                currentPrice, previousClose, dailyChange, dailyChangePercent, currency,
                marketCap, priceToEarnings, priceToBook, evToEbitda, dividendYield,
                returnOnEquity, returnOnAssets, netMargin, operatingMargin,
                netDebt, debtToEquity, totalCash, totalRevenue, ebitda,
                netAssetValue, pvp,
                fiftyTwoWeekHigh, fiftyTwoWeekLow,
                averageVolume, regularMarketVolume,
                userPosition, recentDividends,
                dataSource, lastUpdated, dataStatus
            );
        }
    }
}
