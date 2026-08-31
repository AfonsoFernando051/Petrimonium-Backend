package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.cache.AssetDetailsCache;
import com.jf.PetApp.application.investment.dto.AssetDetailsResponseDTO;
import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.dto.DividendDTO;
import com.jf.PetApp.application.investment.dto.DividendRadarEntryDTO;
import com.jf.PetApp.application.investment.dto.UserPositionDTO;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;
import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import com.jf.PetApp.application.investment.service.AssetDetailsResponseMapper;
import com.jf.PetApp.application.investment.service.UserPositionCalculator;
import com.jf.PetApp.core.domain.Investment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrates the enriched asset details response by combining:
 * <ol>
 *   <li>Enriched market data from the financial data provider</li>
 *   <li>The authenticated user's real position (if they own the asset)</li>
 *   <li>Recent dividend history from the provider</li>
 * </ol>
 *
 * <p>Purely orchestration — the "how do I turn a raw payload into the DTO" decisions live in
 * {@link AssetDetailsResponseMapper}, and the position math lives in
 * {@link UserPositionCalculator}, so both can be tested independently of the cache/HTTP/repo
 * wiring here.</p>
 */
@Service
public class GetAssetDetailsUseCaseImpl implements GetAssetDetailsUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetAssetDetailsUseCaseImpl.class);
    private static final int MAX_RECENT_DIVIDENDS = 12;

    private final ExternalInvestmentApiPort externalApi;
    private final InvestmentRepositoryPort investmentRepo;
    private final AssetDetailsCache cache;
    private final AssetDetailsResponseMapper mapper;
    private final UserPositionCalculator positionCalculator;

    public GetAssetDetailsUseCaseImpl(
            ExternalInvestmentApiPort externalApi,
            InvestmentRepositoryPort investmentRepo,
            AssetDetailsCache cache,
            AssetDetailsResponseMapper mapper,
            UserPositionCalculator positionCalculator
    ) {
        this.externalApi = externalApi;
        this.investmentRepo = investmentRepo;
        this.cache = cache;
        this.mapper = mapper;
        this.positionCalculator = positionCalculator;
    }

    @Override
    public AssetDetailsResponseDTO execute(String email, String ticker) {
        String normalizedTicker = ticker.toUpperCase().trim();

        // 1. Check cache first
        AssetDetailsResponseDTO cached = cache.get(normalizedTicker);
        if (cached != null) {
            // Cached response has market-wide data; recompute user position fresh
            UserPositionDTO userPosition = computeUserPosition(email, normalizedTicker, cached.currentPrice());
            return mapper.withUserPositionAndStatus(cached, userPosition, "CACHED");
        }

        // 2. Fetch enriched quote from provider
        Optional<Map<String, Object>> enrichedOpt = externalApi.getEnrichedQuote(normalizedTicker);

        if (enrichedOpt.isEmpty()) {
            // Fallback: try the simpler getQuote for basic price data
            Optional<AssetQuoteResponse> simpleQuote = externalApi.getQuote(normalizedTicker);
            if (simpleQuote.isEmpty()) {
                UserPositionDTO userPosition = computeUserPosition(email, normalizedTicker, null);
                return mapper.unavailable(normalizedTicker, userPosition);
            }
            UserPositionDTO userPosition = computeUserPosition(email, normalizedTicker, simpleQuote.get().regularMarketPrice());
            List<DividendRadarEntryDTO> dividends = fetchRecentDividends(email, normalizedTicker);
            return mapper.fromSimpleQuote(normalizedTicker, simpleQuote.get(), userPosition, dividends);
        }

        Map<String, Object> data = enrichedOpt.get();
        Double currentPrice = toDouble(data.get("regularMarketPrice"));

        UserPositionDTO userPosition = computeUserPosition(email, normalizedTicker, currentPrice);
        List<DividendRadarEntryDTO> recentDividends = fetchRecentDividends(email, normalizedTicker);

        AssetDetailsResponseDTO result = mapper.fromEnrichedData(normalizedTicker, data, userPosition, recentDividends);

        // 3. Cache (without user position — that's per-user)
        cache.put(normalizedTicker, result);

        return result;
    }

    private static Double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private UserPositionDTO computeUserPosition(String email, String ticker, Double currentPrice) {
        List<Investment> lots = investmentRepo.findByUserEmail(email);
        BigDecimal price = currentPrice == null ? null : BigDecimal.valueOf(currentPrice);
        return positionCalculator.compute(lots, ticker, price);
    }

    private List<DividendRadarEntryDTO> fetchRecentDividends(String email, String ticker) {
        try {
            List<DividendDTO> dividends = externalApi.getDividends(ticker);
            if (dividends.isEmpty()) return List.of();

            // Get user's quantity for this ticker. Kept as double here (unlike the
            // BigDecimal position/summary chain) — DividendRadarEntryDTO/DividendDTO are a
            // separate, still-Double DTO chain sourced from confirmed provider payment
            // history, out of scope for this pass (docs/BACKEND_MODULE_PLAN.md §12).
            List<Investment> lots = investmentRepo.findByUserEmail(email);
            double userQuantity = lots.stream()
                    .filter(lot -> lot.name().equalsIgnoreCase(ticker))
                    .map(Investment::quantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .doubleValue();

            LocalDate today = LocalDate.now();
            List<DividendRadarEntryDTO> entries = new ArrayList<>();

            for (DividendDTO div : dividends) {
                boolean paid = div.paymentDate() != null && !div.paymentDate().isAfter(today);
                String status = paid ? DividendRadarEntryDTO.STATUS_PAID : DividendRadarEntryDTO.STATUS_ANNOUNCED;
                double qty = userQuantity > 0 ? userQuantity : 0;
                entries.add(new DividendRadarEntryDTO(
                    div.ticker(), div.type(), div.rawLabel(), div.ratePerShare(),
                    div.dataCom(), div.paymentDate(), div.approvedOn(),
                    qty, div.ratePerShare() * qty, status
                ));
            }

            // Sort most recent first, cap at MAX_RECENT_DIVIDENDS
            entries.sort((a, b) -> {
                if (a.paymentDate() == null && b.paymentDate() == null) return 0;
                if (a.paymentDate() == null) return 1;
                if (b.paymentDate() == null) return -1;
                return b.paymentDate().compareTo(a.paymentDate());
            });

            return entries.subList(0, Math.min(entries.size(), MAX_RECENT_DIVIDENDS));
        } catch (Exception e) {
            log.warn("Failed to fetch dividends for asset details {}: {}", ticker, e.getMessage());
            return List.of();
        }
    }
}
