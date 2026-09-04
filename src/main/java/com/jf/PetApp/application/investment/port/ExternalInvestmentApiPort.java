package com.jf.PetApp.application.investment.port;

import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.dto.DividendDTO;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.List;

/**
 * Market data from an external provider.
 *
 * <p><strong>Implementations must never fabricate a price.</strong> The same rule
 * {@code RealPortfolioSyncPort} already states for positions applies here, and matters more: a
 * quote flows straight into portfolio valuation, gain/loss and achievement thresholds, so an
 * invented number is shown to the user as their own money. When the provider has nothing to say —
 * unknown ticker, outage, missing credentials — return empty and let the caller decide how to
 * degrade. Empty is a usable answer; a plausible wrong number is not.
 *
 * <p>Where an environment must still serve a placeholder (local development without provider
 * credentials), it has to be marked as simulated so callers can refuse it, never returned as an
 * ordinary quote.
 */
public interface ExternalInvestmentApiPort {

    /** Empty when no quote is available — never a stand-in price. See the type-level rule. */
    Optional<AssetQuoteResponse> getQuote(String ticker);

    /** Empty list when the provider matches nothing — never an invented ticker. */
    List<AssetQuoteResponse> searchQuotes(String query);

    /**
     * The ticker's closing price on the most recent trading day on or before
     * {@code date} — e.g. a Saturday resolves to Friday's close. Empty when
     * the provider has no data that far back (ticker didn't exist yet) or
     * {@code date} is in the future. Never a substituted price from another day — see the
     * type-level rule.
     */
    Optional<AssetQuoteResponse> getQuoteAtDate(String ticker, LocalDate date);

    /**
     * Confirmed cash-dividend/JCP/yield history and announcements for a
     * ticker. Returns an empty list when the provider has nothing to report
     * (unknown ticker, no corporate actions, provider error) — implementations
     * must never fabricate an entry to fill this list.
     */
    List<DividendDTO> getDividends(String ticker);

    /**
     * Returns all available fields from the provider for a given ticker as a
     * raw key-value map. This is the enriched counterpart to {@link #getQuote},
     * which extracts only the minimum needed for portfolio valuation.
     *
     * <p>Implementations should return every field the provider supplies
     * without filtering — the use case layer decides which fields to map
     * into the response DTO. Values that the provider did not return should
     * simply be absent from the map, never fabricated.</p>
     */
    Optional<Map<String, Object>> getEnrichedQuote(String ticker);
}
