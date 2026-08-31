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
import com.jf.PetApp.core.domain.enums.DividendType;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetAssetDetailsUseCaseImplTest {

    private static final String EMAIL = "investor@test.com";
    private static final double DELTA = 0.0001;

    @Mock
    private ExternalInvestmentApiPort externalApi;

    @Mock
    private InvestmentRepositoryPort investmentRepo;

    @Mock
    private AssetDetailsCache cache;

    private GetAssetDetailsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetAssetDetailsUseCaseImpl(
            externalApi, investmentRepo, cache,
            new AssetDetailsResponseMapper(), new UserPositionCalculator()
        );
        when(cache.get(any())).thenReturn(null);
    }

    private Investment lot(String ticker, double quantity, double purchasePrice) {
        return new Investment(1, EMAIL, ticker, quantity, purchasePrice, LocalDate.now(), InvestmentType.STOCKS);
    }

    private Map<String, Object> enrichedData(double price) {
        Map<String, Object> data = new HashMap<>();
        data.put("regularMarketPrice", price);
        data.put("regularMarketPreviousClose", price - 1.0);
        data.put("regularMarketChange", 1.0);
        data.put("regularMarketChangePercent", 2.5);
        data.put("currency", "BRL");
        data.put("shortName", "Petrobras PN");
        return data;
    }

    // ── User-position money math ────────────────────────────────────────

    @Test
    void execute_UserOwnsAsset_ComputesAveragePriceAcrossLots() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of(
                lot("PETR4", 100.0, 30.0),
                lot("PETR4", 50.0, 36.0)
        ));
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.of(enrichedData(35.0)));
        when(externalApi.getDividends("PETR4")).thenReturn(List.of());

        AssetDetailsResponseDTO result = useCase.execute(EMAIL, "PETR4");

        UserPositionDTO position = result.userPosition();
        assertNotNull(position);
        // invested = 100*30 + 50*36 = 3000 + 1800 = 4800; quantity = 150
        assertEquals(150.0, position.quantity(), DELTA);
        assertEquals(4800.0, position.investedValue(), DELTA);
        assertEquals(32.0, position.averagePrice(), DELTA); // 4800 / 150
    }

    @Test
    void execute_UserOwnsAsset_ComputesUnrealizedGainAndPercent() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of(lot("PETR4", 100.0, 30.0)));
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.of(enrichedData(36.0)));
        when(externalApi.getDividends("PETR4")).thenReturn(List.of());

        UserPositionDTO position = useCase.execute(EMAIL, "PETR4").userPosition();

        // invested = 3000, currentValue = 100*36 = 3600, gain = 600, gain% = 20%
        assertEquals(3600.0, position.currentValue(), DELTA);
        assertEquals(600.0, position.unrealizedGain(), DELTA);
        assertEquals(20.0, position.unrealizedGainPercent(), DELTA);
    }

    @Test
    void execute_UserOwnsAssetAtALoss_ComputesNegativeUnrealizedGain() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of(lot("PETR4", 100.0, 40.0)));
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.of(enrichedData(30.0)));
        when(externalApi.getDividends("PETR4")).thenReturn(List.of());

        UserPositionDTO position = useCase.execute(EMAIL, "PETR4").userPosition();

        // invested = 4000, currentValue = 3000, gain = -1000, gain% = -25%
        assertEquals(-1000.0, position.unrealizedGain(), DELTA);
        assertEquals(-25.0, position.unrealizedGainPercent(), DELTA);
    }

    @Test
    void execute_UserHoldsMultipleTickers_ComputesPortfolioWeightForRequestedTicker() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of(
                lot("PETR4", 100.0, 30.0),  // this ticker: 100 * 35 (current) = 3500
                lot("VALE3", 50.0, 60.0)    // other ticker: 50 * 60 (purchase proxy) = 3000
        ));
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.of(enrichedData(35.0)));
        when(externalApi.getDividends("PETR4")).thenReturn(List.of());

        UserPositionDTO position = useCase.execute(EMAIL, "PETR4").userPosition();

        // total portfolio value = 3500 + 3000 = 6500; weight = 3500/6500 * 100
        assertEquals(3500.0 / 6500.0 * 100, position.portfolioWeight(), DELTA);
    }

    @Test
    void execute_UserDoesNotOwnAsset_UserPositionIsNull() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of(lot("VALE3", 50.0, 60.0)));
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.of(enrichedData(35.0)));
        when(externalApi.getDividends("PETR4")).thenReturn(List.of());

        AssetDetailsResponseDTO result = useCase.execute(EMAIL, "PETR4");

        assertNull(result.userPosition());
    }

    @Test
    void execute_UserOwnsAssetButPriceUnavailable_FallsBackToAveragePriceForValuation() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of(lot("PETR4", 10.0, 30.0)));
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.empty());
        when(externalApi.getQuote("PETR4")).thenReturn(Optional.empty());

        AssetDetailsResponseDTO result = useCase.execute(EMAIL, "PETR4");

        // "unavailable" path: no live price, so current value uses averagePrice (30) as price.
        UserPositionDTO position = result.userPosition();
        assertNotNull(position);
        assertEquals(300.0, position.currentValue(), DELTA); // 10 * 30
        assertEquals(0.0, position.unrealizedGain(), DELTA);
    }

    // ── Dividend enrichment ──────────────────────────────────────────────

    @Test
    void execute_EnrichesDividendsWithUserQuantityAndEstimatedAmount() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of(lot("PETR4", 200.0, 30.0)));
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.of(enrichedData(35.0)));
        when(externalApi.getDividends("PETR4")).thenReturn(List.of(
                new DividendDTO("PETR4", DividendType.DIVIDENDO, "Dividendo", 0.5,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15), LocalDate.of(2025, 12, 20))
        ));

        List<DividendRadarEntryDTO> dividends = useCase.execute(EMAIL, "PETR4").recentDividends();

        assertEquals(1, dividends.size());
        DividendRadarEntryDTO entry = dividends.get(0);
        assertEquals(200.0, entry.userQuantity(), DELTA);
        assertEquals(100.0, entry.estimatedGrossAmount(), DELTA); // 0.5 * 200
    }

    @Test
    void execute_DividendPaymentDateInPast_MarkedPaid() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of());
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.of(enrichedData(35.0)));
        when(externalApi.getDividends("PETR4")).thenReturn(List.of(
                new DividendDTO("PETR4", DividendType.DIVIDENDO, "Dividendo", 0.5,
                        LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1), LocalDate.now().minusMonths(2))
        ));

        DividendRadarEntryDTO entry = useCase.execute(EMAIL, "PETR4").recentDividends().get(0);

        assertEquals(DividendRadarEntryDTO.STATUS_PAID, entry.status());
    }

    @Test
    void execute_DividendPaymentDateInFuture_MarkedAnnounced() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of());
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.of(enrichedData(35.0)));
        when(externalApi.getDividends("PETR4")).thenReturn(List.of(
                new DividendDTO("PETR4", DividendType.DIVIDENDO, "Dividendo", 0.5,
                        LocalDate.now(), LocalDate.now().plusMonths(1), LocalDate.now())
        ));

        DividendRadarEntryDTO entry = useCase.execute(EMAIL, "PETR4").recentDividends().get(0);

        assertEquals(DividendRadarEntryDTO.STATUS_ANNOUNCED, entry.status());
    }

    @Test
    void execute_DividendsSortedMostRecentPaymentDateFirst() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of());
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.of(enrichedData(35.0)));
        when(externalApi.getDividends("PETR4")).thenReturn(List.of(
                new DividendDTO("PETR4", DividendType.DIVIDENDO, "Older", 0.3,
                        null, LocalDate.of(2025, 1, 1), null),
                new DividendDTO("PETR4", DividendType.DIVIDENDO, "Newer", 0.4,
                        null, LocalDate.of(2026, 1, 1), null)
        ));

        List<DividendRadarEntryDTO> dividends = useCase.execute(EMAIL, "PETR4").recentDividends();

        assertEquals("Newer", dividends.get(0).rawLabel());
        assertEquals("Older", dividends.get(1).rawLabel());
    }

    @Test
    void execute_DividendProviderThrows_ReturnsEmptyDividendsWithoutPropagating() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of());
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.of(enrichedData(35.0)));
        when(externalApi.getDividends("PETR4")).thenThrow(new RuntimeException("provider down"));

        List<DividendRadarEntryDTO> dividends = useCase.execute(EMAIL, "PETR4").recentDividends();

        assertTrue(dividends.isEmpty());
    }

    // ── Asset-type heuristics ────────────────────────────────────────────

    @Test
    void execute_TickerEndingIn11_DetectedAsFii() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of());
        when(externalApi.getEnrichedQuote("HGLG11")).thenReturn(Optional.of(enrichedData(150.0)));
        when(externalApi.getDividends("HGLG11")).thenReturn(List.of());

        assertEquals("fii", useCase.execute(EMAIL, "HGLG11").assetType());
    }

    @Test
    void execute_TickerEndingIn34_DetectedAsBdr() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of());
        when(externalApi.getEnrichedQuote("AAPL34")).thenReturn(Optional.of(enrichedData(80.0)));
        when(externalApi.getDividends("AAPL34")).thenReturn(List.of());

        assertEquals("bdr", useCase.execute(EMAIL, "AAPL34").assetType());
    }

    @Test
    void execute_TickerEndingIn39_DetectedAsEtf() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of());
        when(externalApi.getEnrichedQuote("SPXI39")).thenReturn(Optional.of(enrichedData(20.0)));
        when(externalApi.getDividends("SPXI39")).thenReturn(List.of());

        assertEquals("etf", useCase.execute(EMAIL, "SPXI39").assetType());
    }

    @Test
    void execute_TickerEndingIn4_DefaultsToStock() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of());
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.of(enrichedData(35.0)));
        when(externalApi.getDividends("PETR4")).thenReturn(List.of());

        assertEquals("stock", useCase.execute(EMAIL, "PETR4").assetType());
    }

    @Test
    void execute_ProviderTypeFieldOverridesTickerHeuristic() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of());
        Map<String, Object> data = enrichedData(35.0);
        data.put("type", "fund");
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.of(data));
        when(externalApi.getDividends("PETR4")).thenReturn(List.of());

        // Ticker suffix "4" would normally mean "stock", but the provider's own type wins.
        assertEquals("fii", useCase.execute(EMAIL, "PETR4").assetType());
    }

    // ── Provider fallback paths ──────────────────────────────────────────

    @Test
    void execute_EnrichedQuoteEmpty_FallsBackToSimpleQuote() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of());
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.empty());
        when(externalApi.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 35.0, "BRL", 1.5)));
        when(externalApi.getDividends("PETR4")).thenReturn(List.of());

        AssetDetailsResponseDTO result = useCase.execute(EMAIL, "PETR4");

        assertEquals(35.0, result.currentPrice());
        assertEquals("PARTIAL", result.dataStatus());
    }

    @Test
    void execute_NoProviderDataAtAll_ReturnsUnavailableStatusWithoutFabricatingData() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of());
        when(externalApi.getEnrichedQuote("UNKNOWN")).thenReturn(Optional.empty());
        when(externalApi.getQuote("UNKNOWN")).thenReturn(Optional.empty());

        AssetDetailsResponseDTO result = useCase.execute(EMAIL, "UNKNOWN");

        assertEquals("UNAVAILABLE", result.dataStatus());
        assertNull(result.currentPrice());
        assertNull(result.marketCap());
        assertEquals(List.of(), result.recentDividends());
    }

    @Test
    void execute_TickerIsNormalizedToUppercaseAndTrimmed() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of());
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.of(enrichedData(35.0)));
        when(externalApi.getDividends("PETR4")).thenReturn(List.of());

        AssetDetailsResponseDTO result = useCase.execute(EMAIL, " petr4 ");

        assertEquals("PETR4", result.ticker());
        verify(externalApi).getEnrichedQuote("PETR4");
    }

    // ── Caching ────────────────────────────────────────────────────────

    @Test
    void execute_CacheHit_RecomputesUserPositionButSkipsProviderCall() {
        AssetDetailsResponseDTO cached = new AssetDetailsResponseDTO(
                "PETR4", "Petrobras PN", null, "stock", null, null, null,
                35.0, null, null, null, "BRL",
                null, null, null, null, null,
                null, null, null, null,
                null, null, null, null, null,
                null, null,
                null, null,
                null, null,
                null, List.of(),
                "brapi.dev", "2026-01-01T00:00:00Z", "FRESH"
        );
        when(cache.get("PETR4")).thenReturn(cached);
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of(lot("PETR4", 10.0, 30.0)));

        AssetDetailsResponseDTO result = useCase.execute(EMAIL, "PETR4");

        assertEquals("CACHED", result.dataStatus());
        assertNotNull(result.userPosition());
        assertEquals(350.0, result.userPosition().currentValue(), DELTA); // 10 * 35 (cached price)
        verify(externalApi, never()).getEnrichedQuote(any());
        verify(externalApi, never()).getQuote(any());
    }

    @Test
    void execute_FreshFetch_StoresResultInCache() {
        when(investmentRepo.findByUserEmail(EMAIL)).thenReturn(List.of());
        when(externalApi.getEnrichedQuote("PETR4")).thenReturn(Optional.of(enrichedData(35.0)));
        when(externalApi.getDividends("PETR4")).thenReturn(List.of());

        useCase.execute(EMAIL, "PETR4");

        verify(cache).put(eq("PETR4"), any(AssetDetailsResponseDTO.class));
    }
}
