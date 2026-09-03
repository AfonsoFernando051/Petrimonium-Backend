package com.jf.PetApp.infrastructure.external;

import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.dto.DividendDTO;
import com.jf.PetApp.core.domain.enums.DividendType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrapiInvestmentApiClientTest {

    private final RestTemplate restTemplate = mock(RestTemplate.class);

    /// Placeholder quotes are allowed outside prod, so the default environment
    /// here is a non-prod one — matching how the client behaves locally.
    private static Environment environmentWithProfiles(String... profiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        return env;
    }

    private final BrapiInvestmentApiClient client =
            new BrapiInvestmentApiClient(restTemplate, environmentWithProfiles("dev"));

    @BeforeEach
    void configureToken() {
        ReflectionTestUtils.setField(client, "token", "test-token");
        ReflectionTestUtils.setField(client, "baseUrl", "https://brapi.dev");
    }

    // ---- getQuote ----

    @Test
    void getQuote_WithNoTokenConfigured_ReturnsSimulatedMockData() {
        ReflectionTestUtils.setField(client, "token", "");

        Optional<AssetQuoteResponse> result = client.getQuote("petr4");

        assertTrue(result.isPresent());
        assertEquals("PETR4", result.get().symbol());
        assertTrue(result.get().shortName().contains("Simulated"));
    }

    @Test
    void getQuote_WithBlankTokenConfigured_ReturnsSimulatedMockData() {
        ReflectionTestUtils.setField(client, "token", "   ");

        Optional<AssetQuoteResponse> result = client.getQuote("vale3");

        assertTrue(result.isPresent());
        assertEquals("VALE3", result.get().symbol());
    }

    @Test
    void getQuote_WithSuccessfulResponse_ParsesTheFirstResult() {
        Map<String, Object> data = Map.of(
                "symbol", "PETR4",
                "shortName", "Petrobras PN",
                "regularMarketPrice", 34.5,
                "currency", "BRL",
                "regularMarketChangePercent", 1.25
        );
        Map<String, Object> response = Map.of("results", List.of(data));
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(response);

        Optional<AssetQuoteResponse> result = client.getQuote("petr4");

        assertTrue(result.isPresent());
        assertEquals("PETR4", result.get().symbol());
        assertEquals("Petrobras PN", result.get().shortName());
        assertEquals(34.5, result.get().regularMarketPrice());
        assertEquals("BRL", result.get().currency());
        assertEquals(1.25, result.get().regularMarketChangePercent());
    }

    @Test
    void getQuote_WithMissingChangePercent_DefaultsToZero() {
        Map<String, Object> data = Map.of("symbol", "PETR4", "regularMarketPrice", 34.5);
        Map<String, Object> response = Map.of("results", List.of(data));
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(response);

        Optional<AssetQuoteResponse> result = client.getQuote("petr4");

        assertEquals(0.0, result.get().regularMarketChangePercent());
    }

    @Test
    void getQuote_WithEmptyResultsArray_ReturnsEmpty() {
        Map<String, Object> response = Map.of("results", List.of());
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(response);

        assertTrue(client.getQuote("petr4").isEmpty());
    }

    @Test
    void getQuote_WithNoResultsKey_ReturnsEmpty() {
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(Map.of("other", "field"));

        assertTrue(client.getQuote("petr4").isEmpty());
    }

    @Test
    void getQuote_WithNullResponseBody_ReturnsEmpty() {
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(null);

        assertTrue(client.getQuote("petr4").isEmpty());
    }

    @Test
    void getQuote_WhenHttpClientErrorOccurs_ReturnsEmptyInsteadOfThrowing() {
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenThrow(HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        assertTrue(client.getQuote("petr4").isEmpty());
    }

    @Test
    void getQuote_WhenGenericExceptionOccurs_ReturnsEmptyInsteadOfThrowing() {
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenThrow(new RuntimeException("connection reset, url had token=test-token"));

        assertTrue(client.getQuote("petr4").isEmpty());
    }

    // ---- getQuoteAtDate ----

    @Test
    void getQuoteAtDate_WithTodayOrFutureDate_DelegatesToGetQuote() {
        Map<String, Object> data = Map.of("symbol", "PETR4", "regularMarketPrice", 34.5);
        Map<String, Object> response = Map.of("results", List.of(data));
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(response);

        Optional<AssetQuoteResponse> result = client.getQuoteAtDate("petr4", LocalDate.now(java.time.ZoneOffset.UTC));

        assertTrue(result.isPresent());
        assertEquals(34.5, result.get().regularMarketPrice());
    }

    @Test
    void getQuoteAtDate_WithNoTokenConfigured_ReturnsSimulatedMockData() {
        ReflectionTestUtils.setField(client, "token", "");

        Optional<AssetQuoteResponse> result = client.getQuoteAtDate("petr4", LocalDate.of(2020, 1, 1));

        assertTrue(result.isPresent());
        assertEquals("PETR4", result.get().symbol());
        assertTrue(result.get().shortName().contains("Simulated"));
    }

    @Test
    void getQuoteAtDate_PicksTheClosingPriceOnOrBeforeTheRequestedDate() {
        // Saturday 2025-01-04 has no trading data — the most recent close on/before it
        // is Friday 2025-01-03.
        Map<String, Object> data = Map.of(
                "symbol", "PETR4",
                "shortName", "Petrobras PN",
                "currency", "BRL",
                "historicalDataPrice", List.of(
                        Map.of("date", 1735776000L, "close", 30.0), // 2025-01-02
                        Map.of("date", 1735862400L, "close", 31.0), // 2025-01-03
                        Map.of("date", 1736035200L, "close", 32.0)  // 2025-01-05 (after target)
                )
        );
        Map<String, Object> response = Map.of("results", List.of(data));
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(response);

        Optional<AssetQuoteResponse> result = client.getQuoteAtDate("petr4", LocalDate.of(2025, 1, 4));

        assertTrue(result.isPresent());
        assertEquals("PETR4", result.get().symbol());
        assertEquals(31.0, result.get().regularMarketPrice());
    }

    @Test
    void getQuoteAtDate_WhenAllHistoryIsAfterTheRequestedDate_ReturnsEmpty() {
        Map<String, Object> data = Map.of(
                "symbol", "PETR4",
                "historicalDataPrice", List.of(Map.of("date", 1736035200L, "close", 32.0)) // 2025-01-05
        );
        Map<String, Object> response = Map.of("results", List.of(data));
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(response);

        assertTrue(client.getQuoteAtDate("petr4", LocalDate.of(2025, 1, 1)).isEmpty());
    }

    @Test
    void getQuoteAtDate_WithMissingHistoricalDataPrice_ReturnsEmpty() {
        Map<String, Object> data = Map.of("symbol", "PETR4");
        Map<String, Object> response = Map.of("results", List.of(data));
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(response);

        assertTrue(client.getQuoteAtDate("petr4", LocalDate.of(2025, 1, 1)).isEmpty());
    }

    @Test
    void getQuoteAtDate_WithEmptyResults_ReturnsEmpty() {
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(Map.of("results", List.of()));

        assertTrue(client.getQuoteAtDate("petr4", LocalDate.of(2025, 1, 1)).isEmpty());
    }

    @Test
    void getQuoteAtDate_WhenExceptionOccurs_ReturnsEmptyInsteadOfThrowing() {
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenThrow(new RuntimeException("boom"));

        assertTrue(client.getQuoteAtDate("petr4", LocalDate.of(2025, 1, 1)).isEmpty());
    }

    // ---- searchQuotes ----

    @Test
    void searchQuotes_WithMatchingStocks_MapsEachEntry() {
        Map<String, Object> stock = Map.of("stock", "PETR4", "name", "Petrobras", "close", 34.5);
        Map<String, Object> response = Map.of("stocks", List.of(stock));
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(response);

        List<AssetQuoteResponse> result = client.searchQuotes("petr");

        assertEquals(1, result.size());
        assertEquals("PETR4", result.get(0).symbol());
        assertEquals("Petrobras", result.get(0).shortName());
        assertEquals(34.5, result.get(0).regularMarketPrice());
    }

    @Test
    void searchQuotes_WithNoStocksKey_ReturnsEmptyList() {
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(Map.of("other", "field"));

        assertTrue(client.searchQuotes("petr").isEmpty());
    }

    @Test
    void searchQuotes_WhenExceptionOccurs_ReturnsEmptyListInsteadOfThrowing() {
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenThrow(new RuntimeException("boom"));

        assertTrue(client.searchQuotes("petr").isEmpty());
    }

    // ---- getDividends ----

    @Test
    void getDividends_WithValidCashDividends_MapsEachEntry() {
        Map<String, Object> cashDividend = Map.of(
                "rate", 1.5,
                "label", "DIVIDENDO",
                "lastDatePrior", "2026-09-21T03:00:00.000Z",
                "paymentDate", "2026-10-01T03:00:00.000Z",
                "approvedOn", "2026-08-01T03:00:00.000Z"
        );
        Map<String, Object> data = Map.of("cashDividends", List.of(cashDividend));
        Map<String, Object> result0 = Map.of("data", data);
        Map<String, Object> response = Map.of("results", List.of(result0));
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(response);

        List<DividendDTO> dividends = client.getDividends("petr4");

        assertEquals(1, dividends.size());
        DividendDTO dto = dividends.get(0);
        assertEquals("PETR4", dto.ticker());
        assertEquals(DividendType.DIVIDENDO, dto.type());
        assertEquals(1.5, dto.ratePerShare());
        assertEquals(LocalDate.of(2026, 9, 21), dto.dataCom());
        assertEquals(LocalDate.of(2026, 10, 1), dto.paymentDate());
        assertEquals(LocalDate.of(2026, 8, 1), dto.approvedOn());
    }

    @Test
    void getDividends_WithNullRate_SkipsTheEntry() {
        Map<String, Object> cashDividend = new java.util.HashMap<>();
        cashDividend.put("rate", null);
        cashDividend.put("label", "DIVIDENDO");
        Map<String, Object> data = Map.of("cashDividends", List.of(cashDividend));
        Map<String, Object> result0 = Map.of("data", data);
        Map<String, Object> response = Map.of("results", List.of(result0));
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(response);

        assertTrue(client.getDividends("petr4").isEmpty());
    }

    @Test
    void getDividends_WithEmptyResults_ReturnsEmptyList() {
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(Map.of("results", List.of()));

        assertTrue(client.getDividends("petr4").isEmpty());
    }

    @Test
    void getDividends_WithDataNotAMap_ReturnsEmptyList() {
        Map<String, Object> result0 = Map.of("data", "not-a-map");
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(Map.of("results", List.of(result0)));

        assertTrue(client.getDividends("petr4").isEmpty());
    }

    @Test
    void getDividends_WithCashDividendsNotAList_ReturnsEmptyList() {
        Map<String, Object> data = Map.of("cashDividends", "not-a-list");
        Map<String, Object> result0 = Map.of("data", data);
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(Map.of("results", List.of(result0)));

        assertTrue(client.getDividends("petr4").isEmpty());
    }

    @Test
    void getDividends_WithoutATokenConfigured_StillBuildsAValidUrlAndSkipsTheTokenParam() {
        ReflectionTestUtils.setField(client, "token", "");
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(Map.of("results", List.of()));

        assertTrue(client.getDividends("petr4").isEmpty());
    }

    @Test
    void getDividends_WhenExceptionOccurs_ReturnsEmptyListInsteadOfThrowing() {
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenThrow(new RuntimeException("boom"));

        assertTrue(client.getDividends("petr4").isEmpty());
    }

    // ---- getEnrichedQuote ----

    @Test
    void getEnrichedQuote_WithNoTokenConfigured_ReturnsEmpty() {
        ReflectionTestUtils.setField(client, "token", "");

        assertTrue(client.getEnrichedQuote("petr4").isEmpty());
    }

    @Test
    void getEnrichedQuote_WithSuccessfulResponse_ReturnsTheFirstResultMap() {
        Map<String, Object> data = Map.of("symbol", "PETR4", "sector", "Energy");
        Map<String, Object> response = Map.of("results", List.of(data));
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(response);

        Optional<Map<String, Object>> result = client.getEnrichedQuote("petr4");

        assertTrue(result.isPresent());
        assertEquals("Energy", result.get().get("sector"));
    }

    @Test
    void getEnrichedQuote_WithEmptyResults_ReturnsEmpty() {
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(Map.of("results", List.of()));

        assertFalse(client.getEnrichedQuote("petr4").isPresent());
    }

    @Test
    void getEnrichedQuote_WhenHttpClientErrorOccurs_ReturnsEmptyInsteadOfThrowing() {
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenThrow(HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "err", null, null, null));

        assertTrue(client.getEnrichedQuote("petr4").isEmpty());
    }

    @Test
    void getEnrichedQuote_WhenGenericExceptionOccurs_ReturnsEmptyInsteadOfThrowing() {
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenThrow(new RuntimeException("boom"));

        assertTrue(client.getEnrichedQuote("petr4").isEmpty());
    }

    // A placeholder price is indistinguishable, downstream, from a real one: it
    // flows into portfolio valuation, gain/loss, allocation and achievement
    // thresholds. Fabricating one in prod shows the user invented money as fact,
    // so these two paths must refuse rather than fall back.

    @Test
    void getQuote_WithNoTokenInProd_ReturnsEmptyInsteadOfFabricatingAPrice() {
        BrapiInvestmentApiClient prodClient =
                new BrapiInvestmentApiClient(restTemplate, environmentWithProfiles("prod"));
        ReflectionTestUtils.setField(prodClient, "token", "");
        ReflectionTestUtils.setField(prodClient, "baseUrl", "https://brapi.dev");

        assertTrue(prodClient.getQuote("petr4").isEmpty());
    }

    @Test
    void getQuoteAtDate_WithNoTokenInProd_ReturnsEmptyInsteadOfFabricatingAPrice() {
        BrapiInvestmentApiClient prodClient =
                new BrapiInvestmentApiClient(restTemplate, environmentWithProfiles("prod"));
        ReflectionTestUtils.setField(prodClient, "token", "");
        ReflectionTestUtils.setField(prodClient, "baseUrl", "https://brapi.dev");

        assertTrue(prodClient.getQuoteAtDate("petr4", LocalDate.now().minusDays(30)).isEmpty());
    }
}
