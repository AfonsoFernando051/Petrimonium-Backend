package com.jf.PetApp.application.investment.dto;

/**
 * A quote as returned by the market-data provider.
 *
 * <p>{@code simulated} marks a placeholder quote that no provider actually produced — see
 * {@code BrapiInvestmentApiClient#mayServePlaceholderQuotes}. It exists so a fabricated price can
 * never be laundered into a real portfolio valuation: consumers that value real money must treat a
 * simulated quote as "no quote at all" rather than as a price
 * (see {@code GetPortfolioHoldingsUseCaseImpl}).
 *
 * <p>The shorter constructors default it to {@code false} — a quote is real unless the code that
 * fabricated it says otherwise, so no caller can forget to opt in and accidentally mark real data
 * as simulated.
 */
public record AssetQuoteResponse(
    String symbol,
    String shortName,
    Double regularMarketPrice,
    String currency,
    Double regularMarketChangePercent,
    boolean simulated
) {
    public AssetQuoteResponse(String symbol, String shortName, Double regularMarketPrice, String currency,
                              Double regularMarketChangePercent) {
        this(symbol, shortName, regularMarketPrice, currency, regularMarketChangePercent, false);
    }

    public AssetQuoteResponse(String symbol, String shortName, Double regularMarketPrice, String currency) {
        this(symbol, shortName, regularMarketPrice, currency, 0.0, false);
    }

    /** A placeholder quote, valid only outside prod. */
    public static AssetQuoteResponse simulated(String symbol, String shortName, Double price, String currency) {
        return new AssetQuoteResponse(symbol, shortName, price, currency, 0.0, true);
    }
}
