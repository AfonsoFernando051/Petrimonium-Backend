package com.jf.PetApp.application.investment.service;

import com.jf.PetApp.application.investment.dto.UserPositionDTO;
import com.jf.PetApp.core.domain.Investment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pure position math for a single ticker within a user's full lot list — extracted from
 * {@code GetAssetDetailsUseCaseImpl} so the calculation (average price, unrealized gain,
 * portfolio weight) can be unit tested without a repository or HTTP call in the way.
 */
@Component
public class UserPositionCalculator {

    /**
     * @param allLots every lot the user owns, across all tickers (needed for portfolio weight)
     * @param ticker the ticker to compute a position for
     * @param currentPrice the live price for {@code ticker}, or {@code null} if unavailable
     *                      (falls back to average purchase price)
     * @return {@code null} if the user holds no lots of {@code ticker}
     */
    public UserPositionDTO compute(List<Investment> allLots, String ticker, Double currentPrice) {
        List<Investment> tickerLots = allLots.stream()
                .filter(lot -> lot.name().equalsIgnoreCase(ticker))
                .toList();

        if (tickerLots.isEmpty()) return null;

        double quantity = tickerLots.stream().mapToDouble(Investment::quantity).sum();
        double investedValue = tickerLots.stream()
                .mapToDouble(lot -> lot.quantity() * lot.purchasePrice())
                .sum();
        double averagePrice = quantity == 0 ? 0.0 : investedValue / quantity;

        double price = currentPrice != null ? currentPrice : averagePrice;
        double currentValue = quantity * price;
        double unrealizedGain = currentValue - investedValue;
        double unrealizedGainPercent = investedValue == 0 ? 0.0 : (unrealizedGain / investedValue) * 100;

        // Portfolio weight: this ticker's value / total portfolio value. For simplicity, other
        // holdings use purchase price as a proxy for current price (the dedicated holdings
        // endpoint computes this more accurately with live quotes for every ticker).
        double totalPortfolioValue = allLots.stream()
                .mapToDouble(lot -> {
                    if (lot.name().equalsIgnoreCase(ticker) && currentPrice != null) {
                        return lot.quantity() * currentPrice;
                    }
                    return lot.quantity() * lot.purchasePrice();
                })
                .sum();
        double portfolioWeight = totalPortfolioValue == 0 ? 0.0 : (currentValue / totalPortfolioValue) * 100;

        return new UserPositionDTO(
            quantity, averagePrice, investedValue, currentValue,
            unrealizedGain, unrealizedGainPercent, portfolioWeight
        );
    }
}
