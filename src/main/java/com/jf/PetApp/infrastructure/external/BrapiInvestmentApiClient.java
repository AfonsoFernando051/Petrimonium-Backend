package com.jf.PetApp.infrastructure.external;

import com.jf.PetApp.application.common.util.RawFieldExtractor;
import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.dto.DividendDTO;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;
import com.jf.PetApp.core.domain.enums.DividendType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

@Service
public class BrapiInvestmentApiClient implements ExternalInvestmentApiPort {

    private static final Logger log = LoggerFactory.getLogger(BrapiInvestmentApiClient.class);

    private final RestTemplate restTemplate;

    @Value("${api.brapi.token:}")
    private String token;

    @Value("${api.brapi.baseUrl:https://brapi.dev}")
    private String baseUrl;

    private final Environment environment;

    public BrapiInvestmentApiClient(RestTemplate restTemplate, Environment environment) {
        this.restTemplate = restTemplate;
        this.environment = environment;
    }

    /**
     * Whether a missing {@code api.brapi.token} may be answered with placeholder
     * quote data.
     *
     * <p>Outside prod this keeps the app usable without a provider account, which
     * is what {@code .env.example} means by "leave blank to fall back to mock
     * quote data locally". In prod it must never happen: a placeholder price is
     * indistinguishable, downstream, from a real one — it flows into portfolio
     * valuation, gain/loss, allocation and achievement thresholds, and the user
     * is shown fabricated money as fact. {@link #getEnrichedQuote} already
     * refuses to fabricate for exactly this reason; these two quote paths now
     * behave the same way.</p>
     *
     * <p>Returning empty rather than failing the request keeps the degradation
     * honest: callers already handle "no quote available" (see
     * {@code UserPositionCalculator}), and no fabricated number reaches the
     * user either way.</p>
     */
    private boolean mayServePlaceholderQuotes() {
        return !environment.acceptsProfiles(Profiles.of("prod"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Every Brapi quote-style endpoint (getQuote, getDividends, getEnrichedQuote) wraps its
     * payload the same way: a top-level {@code results} array, empty or absent when the
     * provider has nothing to report. Centralizing the "get → check key → cast → check-empty"
     * steps here means each caller only has to decide what to do with the first result.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractResults(Map<String, Object> response) {
        if (response == null || !response.containsKey("results")) {
            return List.of();
        }
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        return results != null ? results : List.of();
    }

    @Override
    public Optional<AssetQuoteResponse> getQuote(String ticker) {
        if (token == null || token.isBlank()) {
            if (!mayServePlaceholderQuotes()) {
                log.error("api.brapi.token is not configured in prod; refusing to serve placeholder"
                        + " quote data for {}. Real portfolio values cannot be computed.", ticker);
                return Optional.empty();
            }
            log.warn("api.brapi.token is not configured; returning mock data for {}", ticker);
            return Optional.of(AssetQuoteResponse.simulated(ticker.toUpperCase(), "Simulated " + ticker.toUpperCase(), 50.0, "BRL"));
        }

        try {
            String url = String.format("%s/api/quote/%s?token=%s", baseUrl, encode(ticker), encode(token));
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            List<Map<String, Object>> results = extractResults(response);
            if (results.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> data = results.get(0);
            String symbol = (String) data.getOrDefault("symbol", ticker);
            String shortName = (String) data.getOrDefault("shortName", "");
            Double price = RawFieldExtractor.toDouble(data.get("regularMarketPrice"));
            String currency = (String) data.getOrDefault("currency", "BRL");
            Double changePercent = RawFieldExtractor.toDouble(data.get("regularMarketChangePercent"));

            return Optional.of(new AssetQuoteResponse(
                    symbol, shortName, price, currency, changePercent != null ? changePercent : 0.0));
        } catch (HttpClientErrorException e) {
            log.warn("Brapi quote request failed for {}: HTTP {}", ticker, e.getStatusCode());
            return Optional.empty();
        } catch (Exception e) {
            // Don't log e.getMessage()/stack trace here: connectivity exceptions from
            // RestTemplate commonly embed the full request URL, which includes the token.
            log.warn("Brapi quote request failed for {}: {}", ticker, e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * Brapi's historical-range buckets (range=1d,5d,1mo,3mo,6mo,1y,2y,5y,10y,max) — picks
     * the smallest bucket that reaches back far enough to contain {@code daysAgo}.
     */
    private static String rangeFor(long daysAgo) {
        if (daysAgo <= 5) return "5d";
        if (daysAgo <= 30) return "1mo";
        if (daysAgo <= 90) return "3mo";
        if (daysAgo <= 180) return "6mo";
        if (daysAgo <= 365) return "1y";
        if (daysAgo <= 730) return "2y";
        if (daysAgo <= 1825) return "5y";
        if (daysAgo <= 3650) return "10y";
        return "max";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<AssetQuoteResponse> getQuoteAtDate(String ticker, LocalDate date) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (date == null || !date.isBefore(today)) {
            return getQuote(ticker);
        }

        if (token == null || token.isBlank()) {
            if (!mayServePlaceholderQuotes()) {
                log.error("api.brapi.token is not configured in prod; refusing to serve placeholder"
                        + " historical quote for {} at {}", ticker, date);
                return Optional.empty();
            }
            log.warn("api.brapi.token is not configured; returning mock data for {} at {}", ticker, date);
            return Optional.of(AssetQuoteResponse.simulated(ticker.toUpperCase(), "Simulated " + ticker.toUpperCase(), 50.0, "BRL"));
        }

        try {
            String range = rangeFor(java.time.temporal.ChronoUnit.DAYS.between(date, today));
            String url = String.format("%s/api/quote/%s?range=%s&interval=1d&token=%s",
                    baseUrl, encode(ticker), range, encode(token));
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            List<Map<String, Object>> results = extractResults(response);
            if (results.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> data = results.get(0);
            Object historyObj = data.get("historicalDataPrice");
            if (!(historyObj instanceof List)) {
                return Optional.empty();
            }
            List<Map<String, Object>> history = (List<Map<String, Object>>) historyObj;

            Map<String, Object> closest = null;
            LocalDate closestDate = null;
            for (Map<String, Object> point : history) {
                LocalDate pointDate = toLocalDateFromEpochSeconds(point.get("date"));
                if (pointDate == null || pointDate.isAfter(date)) continue;
                if (closestDate == null || pointDate.isAfter(closestDate)) {
                    closestDate = pointDate;
                    closest = point;
                }
            }
            if (closest == null) {
                return Optional.empty();
            }

            String symbol = (String) data.getOrDefault("symbol", ticker);
            String shortName = (String) data.getOrDefault("shortName", "");
            Double price = RawFieldExtractor.toDouble(closest.get("close"));
            String currency = (String) data.getOrDefault("currency", "BRL");

            return Optional.of(new AssetQuoteResponse(symbol, shortName, price, currency));
        } catch (HttpClientErrorException e) {
            log.warn("Brapi historical quote request failed for {}: HTTP {}", ticker, e.getStatusCode());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Brapi historical quote request failed for {}: {}", ticker, e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static LocalDate toLocalDateFromEpochSeconds(Object value) {
        Double seconds = RawFieldExtractor.toDouble(value);
        if (seconds == null) return null;
        return Instant.ofEpochSecond(seconds.longValue()).atZone(ZoneOffset.UTC).toLocalDate();
    }

    @Override
    public List<AssetQuoteResponse> searchQuotes(String query) {
        try {
            // we can use search without a token
            String url = String.format("%s/api/quote/list?search=%s", baseUrl, encode(query));
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            List<AssetQuoteResponse> resultList = new ArrayList<>();
            if (response != null && response.containsKey("stocks")) {
                List<Map<String, Object>> stocks = (List<Map<String, Object>>) response.get("stocks");
                if (stocks != null) {
                    for (Map<String, Object> stock : stocks) {
                        String symbol = (String) stock.getOrDefault("stock", "");
                        String name = (String) stock.getOrDefault("name", "");
                        
                        Object priceObj = stock.get("close");
                        Double price = null;
                        if (priceObj instanceof Number) {
                            price = ((Number) priceObj).doubleValue();
                        }
                        
                        resultList.add(new AssetQuoteResponse(symbol, name, price, "BRL"));
                    }
                }
            }
            return resultList;
        } catch (Exception e) {
            // No token in this URL (search doesn't require one), so the message is safe to log.
            log.warn("Brapi search request failed for query '{}': {}", query, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DividendDTO> getDividends(String ticker) {
        List<DividendDTO> result = new ArrayList<>();
        try {
            String url = String.format("%s/api/v2/stocks/dividends?symbols=%s", baseUrl, encode(ticker.toUpperCase()));
            if (token != null && !token.isBlank()) {
                url += "&token=" + encode(token);
            }
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            List<Map<String, Object>> results = extractResults(response);
            if (results.isEmpty()) {
                return result;
            }

            Object dataObj = results.get(0).get("data");
            if (!(dataObj instanceof Map)) {
                return result;
            }
            Map<String, Object> data = (Map<String, Object>) dataObj;

            Object cashDividendsObj = data.get("cashDividends");
            if (!(cashDividendsObj instanceof List)) {
                return result;
            }
            List<Map<String, Object>> cashDividends = (List<Map<String, Object>>) cashDividendsObj;

            for (Map<String, Object> entry : cashDividends) {
                Double rate = RawFieldExtractor.toDouble(entry.get("rate"));
                // Never report a payment we can't confirm a value for.
                if (rate == null) continue;

                String rawLabel = (String) entry.get("label");
                result.add(new DividendDTO(
                        ticker.toUpperCase(),
                        DividendType.fromRawLabel(rawLabel),
                        rawLabel,
                        rate,
                        toLocalDate(entry.get("lastDatePrior")),
                        toLocalDate(entry.get("paymentDate")),
                        toLocalDate(entry.get("approvedOn"))
                ));
            }
            return result;
        } catch (Exception e) {
            // This URL may include the token; log the exception type only, never its message.
            log.warn("Failed to fetch dividends from Brapi for {}: {}", ticker, e.getClass().getSimpleName());
            return new ArrayList<>();
        }
    }

    /**
     * Brapi encodes each date as Brazilian local midnight serialized in UTC
     * (e.g. {@code 2026-09-21T03:00:00.000Z} = 2026-09-21 00:00 America/Sao_Paulo),
     * so reading the UTC calendar date back out yields the correct local date.
     */
    private static LocalDate toLocalDate(Object value) {
        if (!(value instanceof String) || ((String) value).isBlank()) return null;
        try {
            return Instant.parse((String) value).atZone(ZoneOffset.UTC).toLocalDate();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public Optional<Map<String, Object>> getEnrichedQuote(String ticker) {
        if (token == null || token.isBlank()) {
            log.warn("api.brapi.token is not configured; cannot fetch enriched quote for {}", ticker);
            return Optional.empty();
        }

        try {
            // Request all available modules from Brapi for maximum data coverage.
            // The summaryProfile module provides sector, industry, description, etc.
            String url = String.format(
                "%s/api/quote/%s?token=%s&modules=summaryProfile",
                baseUrl, encode(ticker), encode(token)
            );
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            List<Map<String, Object>> results = extractResults(response);
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (HttpClientErrorException e) {
            log.warn("Brapi enriched quote request failed for {}: HTTP {}", ticker, e.getStatusCode());
            return Optional.empty();
        } catch (Exception e) {
            // Don't log e.getMessage(): connectivity exceptions may embed the token in the URL.
            log.warn("Brapi enriched quote request failed for {}: {}", ticker, e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}

