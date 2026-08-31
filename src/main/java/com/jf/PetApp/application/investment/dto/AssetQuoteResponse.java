package com.jf.PetApp.application.investment.dto;

public record AssetQuoteResponse(
    String symbol,
    String shortName,
    Double regularMarketPrice,
    String currency,
    Double regularMarketChangePercent
) {
    public AssetQuoteResponse(String symbol, String shortName, Double regularMarketPrice, String currency) {
        this(symbol, shortName, regularMarketPrice, currency, 0.0);
    }
}
