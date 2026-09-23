package com.jf.PetApp.infrastructure.controller.academy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;

/** Real HTTP, JWT authentication, use cases and H2 persistence; only market data is replaced. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AcademyJourneyIntegrationTest {
    @Autowired private TestRestTemplate http;
    @MockitoBean private ExternalInvestmentApiPort marketData;
    private String academy;
    private String otherAcademy;
    private String wallet;
    private String health;
    private String historicalStudent;

    @BeforeAll
    void registerAndAuthenticateRealAccounts() {
        // Three registrations and five logins stay within the real per-IP rate limit.
        String email = register();
        academy = login(email, "academy");
        wallet = login(email, "wallet");
        health = login(email, "health");
        otherAcademy = login(register(), "academy");
        historicalStudent = login(register(), "academy");
    }

    @Test
    void lessonCompletionPersistsIsIdempotentAndBelongsToTheAuthenticatedStudent() {
        Map<String, Object> catalog = object(get("/api/v1/academy/catalog?lang=pt", academy));
        String lesson = (String) entries(catalog, "lessons").getFirst().get("id");
        String endpoint = "/api/v1/learning/lessons/" + lesson + "/complete";
        assertTrue(values(object(get("/api/v1/learning/progress", academy)), "completedLessonIds").isEmpty());
        Map<String, Object> first = object(call(endpoint, HttpMethod.POST, academy, Map.of("perfectFirstTry", false)));
        assertEquals(false, first.get("alreadyCompleted"));
        Map<String, Object> retry = object(call(endpoint, HttpMethod.POST, academy, Map.of("perfectFirstTry", false)));
        assertEquals(true, retry.get("alreadyCompleted"));
        assertEquals(0, ((Number) retry.get("xpAwarded")).intValue());
        assertEquals(first.get("totalXp"), retry.get("totalXp"));
        object(call(endpoint, HttpMethod.POST, academy, Map.of("perfectFirstTry", true)));
        Map<String, Object> persisted = object(get("/api/v1/learning/progress", academy));
        assertEquals(List.of(lesson), values(persisted, "completedLessonIds"));
        assertTrue(values(persisted, "perfectLessonIds").contains(lesson));
        assertTrue(values(object(get("/api/v1/learning/progress", otherAcademy)), "completedLessonIds").isEmpty());
        assertEquals(HttpStatus.BAD_REQUEST, call("/api/v1/learning/lessons/not-a-lesson/complete",
                HttpMethod.POST, academy, Map.of()).getStatusCode());
        assertEquals(persisted, object(get("/api/v1/learning/progress", academy)));
    }

    @Test
    void simulatorCompletionSurvivesNewRequestsWithoutDuplicatingProgress() {
        String endpoint = "/api/v1/lab/simulators/compound_interest/complete";
        assertEquals(false, object(call(endpoint, HttpMethod.POST, academy, Map.of())).get("alreadyCompleted"));
        Map<String, Object> retry = object(call(endpoint, HttpMethod.POST, academy, Map.of()));
        assertEquals(true, retry.get("alreadyCompleted"));
        assertEquals(0, ((Number) retry.get("xpAwarded")).intValue());
        assertEquals(List.of("compound_interest"), values(object(get("/api/v1/lab/simulators/progress", academy)), "completedSimulatorIds"));
        assertTrue(values(object(get("/api/v1/lab/simulators/progress", otherAcademy)), "completedSimulatorIds").isEmpty());
        assertEquals(HttpStatus.BAD_REQUEST, call("/api/v1/lab/simulators/unknown/complete", HttpMethod.POST, academy, Map.of()).getStatusCode());
    }

    @Test
    void academyCountryIsAlwaysBrazilWithoutOverwritingOtherProductsPreference() {
        object(call("/api/settings/country", HttpMethod.PUT, wallet, Map.of("countryCode", "PT")));
        assertEquals("BR", object(get("/api/settings/country", academy)).get("countryCode"));
        object(call("/api/settings/country", HttpMethod.PUT, academy, Map.of("countryCode", "US")));
        assertEquals("BR", object(get("/api/settings/country", academy)).get("countryCode"));
        assertEquals("PT", object(get("/api/settings/country", wallet)).get("countryCode"));
        assertEquals("PT", object(get("/api/settings/country", health)).get("countryCode"));
    }

    @Test
    void simulatedOrdersPersistRetrySafelyAndResetOnlyTheOwnersPortfolio() {
        when(marketData.getQuote("PETR4")).thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 30.0, "BRL")));
        String base = "/api/v1/simulated-portfolios";
        Map<String, Object> buy = Map.of("ticker", "PETR4", "side", "BUY", "quantity", 10, "clientOrderId", "integration-buy");
        ResponseEntity<Object> response = call(base + "/orders", HttpMethod.POST, academy, buy);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Map<String, Object> first = object(response);
        assertEquals(first.get("id"), object(call(base + "/orders", HttpMethod.POST, academy, buy)).get("id"));
        assertEquals(1, ((List<?>) get(base + "/orders", academy).getBody()).size());
        assertEquals(10.0, ((Number) entries(object(get(base + "/me", academy)), "positions").getFirst().get("quantity")).doubleValue());
        assertTrue(entries(object(get(base + "/me", otherAcademy)), "positions").isEmpty());
        Map<String, Object> other = object(call(base + "/orders", HttpMethod.POST, otherAcademy, buy));
        assertNotEquals(first.get("id"), other.get("id"));
        object(call(base + "/orders", HttpMethod.POST, academy,
                Map.of("ticker", "PETR4", "side", "SELL", "quantity", 4, "clientOrderId", "integration-sell")));
        assertEquals(6.0, ((Number) entries(object(get(base + "/me", academy)), "positions").getFirst().get("quantity")).doubleValue());
        assertEquals(HttpStatus.BAD_REQUEST, call(base + "/orders", HttpMethod.POST, academy,
                Map.of("ticker", "PETR4", "side", "SELL", "quantity", 7)).getStatusCode());
        assertEquals(2, ((List<?>) get(base + "/orders", academy).getBody()).size());
        assertEquals(6.0, ((Number) entries(object(get(base + "/me", academy)), "positions").getFirst().get("quantity")).doubleValue());
        assertEquals(HttpStatus.BAD_REQUEST, call(base + "/reset", HttpMethod.POST, academy, Map.of("confirm", false)).getStatusCode());
        assertEquals(2, ((List<?>) get(base + "/orders", academy).getBody()).size());
        assertEquals(HttpStatus.NO_CONTENT, call(base + "/reset", HttpMethod.POST, academy, Map.of("confirm", true)).getStatusCode());
        assertTrue(entries(object(get(base + "/me", academy)), "positions").isEmpty());
        assertTrue(((List<?>) get(base + "/orders", academy).getBody()).isEmpty());
        assertEquals(1, ((List<?>) get(base + "/orders", otherAcademy).getBody()).size());
        assertEquals(10.0, ((Number) entries(object(get(base + "/me", otherAcademy)), "positions").getFirst().get("quantity")).doubleValue());
    }

    @Test
    void historicalOrderUsesItsTradeDateAndUnavailableQuotesLeaveNoPositionOrOrder() {
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        when(marketData.getQuote("VALE3")).thenReturn(Optional.of(new AssetQuoteResponse("VALE3", "Vale", 90.0, "BRL")));
        when(marketData.getQuoteAtDate("VALE3", yesterday)).thenReturn(Optional.of(new AssetQuoteResponse("VALE3", "Vale", 60.0, "BRL")));
        when(marketData.getQuote("UNAVAILABLE")).thenReturn(Optional.empty());
        when(marketData.getQuote("PLACEHOLDER")).thenReturn(Optional.of(AssetQuoteResponse.simulated("PLACEHOLDER", "Placeholder", 10.0, "BRL")));
        String base = "/api/v1/simulated-portfolios";
        Map<String, Object> order = object(call(base + "/orders", HttpMethod.POST, historicalStudent,
                Map.of("ticker", "VALE3", "side", "BUY", "quantity", 2, "tradeDate", yesterday.toString())));
        assertEquals(60.0, ((Number) order.get("price")).doubleValue());
        assertEquals(120.0, ((Number) order.get("total")).doubleValue());
        assertTrue(((String) order.get("executedAt")).startsWith(yesterday.toString()));
        Map<String, Object> before = object(get(base + "/me", historicalStudent));
        for (String ticker : List.of("UNAVAILABLE", "PLACEHOLDER")) {
            assertEquals(HttpStatus.BAD_REQUEST, call(base + "/orders", HttpMethod.POST, historicalStudent,
                    Map.of("ticker", ticker, "side", "BUY", "quantity", 1)).getStatusCode());
        }
        assertEquals(before, object(get(base + "/me", historicalStudent)));
        assertEquals(1, ((List<?>) get(base + "/orders", historicalStudent).getBody()).size());
    }

    @Test
    void otherProductTokensCannotReadOrMutateAcademyData() {
        for (String token : List.of(wallet, health)) {
            for (String path : List.of("/api/v1/academy/catalog", "/api/v1/learning/progress", "/api/v1/lab/simulators/progress", "/api/v1/simulated-portfolios/me")) {
                assertEquals(HttpStatus.FORBIDDEN, get(path, token).getStatusCode(), path);
            }
            assertEquals(HttpStatus.FORBIDDEN, call("/api/v1/simulated-portfolios/reset", HttpMethod.POST, token, Map.of("confirm", true)).getStatusCode());
            assertEquals(HttpStatus.FORBIDDEN, call("/api/v1/lab/simulators/inflation/complete", HttpMethod.POST, token, Map.of()).getStatusCode());
        }
    }

    private String register() {
        String email = "academy-integration-" + UUID.randomUUID() + "@test.com";
        assertEquals(HttpStatus.CREATED, call("/auth/register", HttpMethod.POST, null,
                Map.of("email", email, "password", "Str0ngPass1", "username", "Integration Student")).getStatusCode());
        return email;
    }

    private String login(String email, String context) {
        return (String) object(call("/auth/login", HttpMethod.POST, null,
                Map.of("email", email, "password", "Str0ngPass1", "appContext", context))).get("accessToken");
    }

    private ResponseEntity<Object> get(String path, String token) {
        return call(path, HttpMethod.GET, token, null);
    }

    private ResponseEntity<Object> call(String path, HttpMethod method, String token, Map<String, ?> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        return http.exchange(path, method, new HttpEntity<>(body, headers), Object.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(ResponseEntity<Object> response) {
        assertTrue(response.getStatusCode().is2xxSuccessful(), () -> response.toString());
        return (Map<String, Object>) response.getBody();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> entries(Map<String, Object> object, String key) {
        return (List<Map<String, Object>>) object.get(key);
    }

    private List<?> values(Map<String, Object> object, String key) {
        return (List<?>) object.get(key);
    }
}
