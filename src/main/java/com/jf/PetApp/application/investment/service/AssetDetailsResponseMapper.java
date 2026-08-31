package com.jf.PetApp.application.investment.service;

import com.jf.PetApp.application.common.util.RawFieldExtractor;
import com.jf.PetApp.application.investment.dto.AssetDetailsResponseDTO;
import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.dto.DividendRadarEntryDTO;
import com.jf.PetApp.application.investment.dto.UserPositionDTO;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link AssetDetailsResponseDTO} from the provider's raw shapes — extracted from
 * {@code GetAssetDetailsUseCaseImpl}, which now only orchestrates (cache → fetch → position →
 * dividends) and delegates every "how do I turn this payload into the DTO" decision here.
 *
 * <p>Never fabricates, estimates or hallucinates a value: a field the provider didn't return
 * stays {@code null} all the way through.</p>
 */
@Component
public class AssetDetailsResponseMapper {

    /** Full enriched-quote path: every field the provider returned, mapped onto the DTO. */
    public AssetDetailsResponseDTO fromEnrichedData(
            String ticker,
            Map<String, Object> data,
            UserPositionDTO userPosition,
            List<DividendRadarEntryDTO> recentDividends
    ) {
        String assetType = detectAssetType(ticker, data);

        String sector = RawFieldExtractor.toStringOrNull(data.get("sector"));
        String industry = RawFieldExtractor.toStringOrNull(data.get("industry"));
        String longName = RawFieldExtractor.toStringOrNull(data.get("longName"));

        // Brapi nests sector/industry/longName under a summaryProfile module when requested.
        @SuppressWarnings("unchecked")
        Map<String, Object> summaryProfile = data.get("summaryProfile") instanceof Map
                ? (Map<String, Object>) data.get("summaryProfile")
                : null;
        if (summaryProfile != null) {
            if (sector == null) sector = RawFieldExtractor.toStringOrNull(summaryProfile.get("sector"));
            if (industry == null) industry = RawFieldExtractor.toStringOrNull(summaryProfile.get("industry"));
            if (longName == null) longName = RawFieldExtractor.toStringOrNull(summaryProfile.get("longName"));
        }

        return AssetDetailsResponseDTO.builder()
            .ticker(ticker)
            .shortName(RawFieldExtractor.toStringOrNull(data.get("shortName")))
            .longName(longName)
            .assetType(assetType)
            .sector(sector)
            .industry(industry)
            .logoUrl(RawFieldExtractor.toStringOrNull(data.get("logourl")))
            .currentPrice(RawFieldExtractor.toDouble(data.get("regularMarketPrice")))
            .previousClose(RawFieldExtractor.toDouble(data.get("regularMarketPreviousClose")))
            .dailyChange(RawFieldExtractor.toDouble(data.get("regularMarketChange")))
            .dailyChangePercent(RawFieldExtractor.toDouble(data.get("regularMarketChangePercent")))
            .currency(RawFieldExtractor.toStringOrNull(data.get("currency")))
            .marketCap(RawFieldExtractor.toDouble(data.get("marketCap")))
            .priceToEarnings(RawFieldExtractor.toDouble(data.get("priceEarnings")))
            .priceToBook(RawFieldExtractor.toDouble(data.get("priceToBook")))
            .evToEbitda(RawFieldExtractor.toDouble(data.get("evToEbitda")))
            .dividendYield(RawFieldExtractor.toDouble(data.get("dividendYield")))
            .returnOnEquity(RawFieldExtractor.toDouble(data.get("returnOnEquity")))
            .returnOnAssets(RawFieldExtractor.toDouble(data.get("returnOnAssets")))
            .netMargin(RawFieldExtractor.toDouble(data.get("netMargin")))
            .operatingMargin(RawFieldExtractor.toDouble(data.get("operatingMargin")))
            .netDebt(RawFieldExtractor.toDouble(data.get("netDebt")))
            .debtToEquity(RawFieldExtractor.toDouble(data.get("debtToEquity")))
            .totalCash(RawFieldExtractor.toDouble(data.get("totalCash")))
            .totalRevenue(RawFieldExtractor.toDouble(data.get("totalRevenue")))
            .ebitda(RawFieldExtractor.toDouble(data.get("ebitda")))
            .netAssetValue(RawFieldExtractor.toDouble(data.get("netAssetValue")))
            .pvp(RawFieldExtractor.toDouble(data.get("priceToBook"))) // P/VP for FIIs
            .fiftyTwoWeekHigh(RawFieldExtractor.toDouble(data.get("fiftyTwoWeekHigh")))
            .fiftyTwoWeekLow(RawFieldExtractor.toDouble(data.get("fiftyTwoWeekLow")))
            .averageVolume(RawFieldExtractor.toLong(data.get("averageDailyVolume3Month")))
            .regularMarketVolume(RawFieldExtractor.toLong(data.get("regularMarketVolume")))
            .userPosition(userPosition)
            .recentDividends(recentDividends)
            .dataSource("brapi.dev")
            .lastUpdated(Instant.now().toString())
            .dataStatus("FRESH")
            .build();
    }

    /** Fallback when only the basic quote endpoint returned data (no enriched fundamentals). */
    public AssetDetailsResponseDTO fromSimpleQuote(
            String ticker,
            AssetQuoteResponse quote,
            UserPositionDTO userPosition,
            List<DividendRadarEntryDTO> recentDividends
    ) {
        return AssetDetailsResponseDTO.builder()
            .ticker(ticker)
            .shortName(quote.shortName())
            .assetType(detectAssetTypeFromTicker(ticker))
            .currentPrice(quote.regularMarketPrice())
            .dailyChangePercent(quote.regularMarketChangePercent())
            .currency(quote.currency())
            .userPosition(userPosition)
            .recentDividends(recentDividends)
            .dataSource("brapi.dev")
            .lastUpdated(Instant.now().toString())
            .dataStatus("PARTIAL")
            .build();
    }

    /** Neither endpoint returned anything — every market field stays null. */
    public AssetDetailsResponseDTO unavailable(String ticker, UserPositionDTO userPosition) {
        return AssetDetailsResponseDTO.builder()
            .ticker(ticker)
            .assetType(detectAssetTypeFromTicker(ticker))
            .userPosition(userPosition)
            .dataSource("unavailable")
            .lastUpdated(Instant.now().toString())
            .dataStatus("UNAVAILABLE")
            .build();
    }

    /** Same market data, fresh per-user position and status (cache-hit path). */
    public AssetDetailsResponseDTO withUserPositionAndStatus(
            AssetDetailsResponseDTO base, UserPositionDTO userPosition, String status) {
        return base.toBuilder()
            .userPosition(userPosition)
            .dataStatus(status)
            .build();
    }

    /**
     * Detects asset type from the Brapi response data combined with ticker pattern heuristics
     * (Brazilian tickers follow predictable patterns).
     */
    private String detectAssetType(String ticker, Map<String, Object> data) {
        String brapiType = RawFieldExtractor.toStringOrNull(data.get("type"));
        if (brapiType != null) {
            String lower = brapiType.toLowerCase();
            if (lower.contains("fund") || lower.contains("fii") || lower.contains("fdo")) return "fii";
            if (lower.contains("etf") || lower.contains("index")) return "etf";
            if (lower.contains("bdr")) return "bdr";
            if (lower.contains("stock") || lower.contains("equity")) return "stock";
        }
        return detectAssetTypeFromTicker(ticker);
    }

    /**
     * Heuristic: Brazilian ticker suffixes. 11 = FII/ETF, 3/4 = stock, 34/35 = BDR.
     */
    private String detectAssetTypeFromTicker(String ticker) {
        if (ticker.length() >= 5) {
            String suffix = ticker.substring(ticker.length() - 2);
            if ("11".equals(suffix)) {
                // Common FIIs: XPML11, HGLG11, etc. Some ETFs also end in 11: IVVB11, BOVA11.
                return "fii"; // Default to FII; the enriched data can override later.
            }
            if ("34".equals(suffix) || "35".equals(suffix)) return "bdr";
            if ("39".equals(suffix)) return "etf";
        }
        return "stock"; // Default assumption.
    }
}
