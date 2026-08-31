package com.jf.PetApp.application.investment.service;

import com.jf.PetApp.application.investment.dto.UserPositionDTO;
import com.jf.PetApp.core.domain.Investment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Pure position math for a single ticker within a user's full lot list — extracted from
 * {@code GetAssetDetailsUseCaseImpl} so the calculation (average price, unrealized gain,
 * portfolio weight) can be unit tested without a repository or HTTP call in the way.
 *
 * <p>All arithmetic is {@link BigDecimal} — this is real money, never {@code double} — scaled
 * to 2 decimal places (money) with {@link RoundingMode#HALF_UP}, matching the rest of
 * real_portfolio and simulated_portfolio's own convention.</p>
 */
@Component
public class UserPositionCalculator {

    private static final int MONEY_SCALE = 2;

    /**
     * @param allLots every lot the user owns, across all tickers (needed for portfolio weight)
     * @param ticker the ticker to compute a position for
     * @param currentPrice the live price for {@code ticker}, or {@code null} if unavailable
     *                      (falls back to average purchase price)
     * @return {@code null} if the user holds no lots of {@code ticker}
     */
    public UserPositionDTO compute(List<Investment> allLots, String ticker, BigDecimal currentPrice) {
        List<Investment> tickerLots = allLots.stream()
                .filter(lot -> lot.name().equalsIgnoreCase(ticker))
                .toList();

        if (tickerLots.isEmpty()) return null;

        BigDecimal quantity = tickerLots.stream()
                .map(Investment::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal investedValue = tickerLots.stream()
                .map(lot -> lot.quantity().multiply(lot.purchasePrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal averagePrice = quantity.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : investedValue.divide(quantity, MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal price = currentPrice != null ? currentPrice : averagePrice;
        BigDecimal currentValue = quantity.multiply(price).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal unrealizedGain = currentValue.subtract(investedValue);
        BigDecimal unrealizedGainPercent = investedValue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : unrealizedGain.multiply(BigDecimal.valueOf(100)).divide(investedValue, MONEY_SCALE, RoundingMode.HALF_UP);

        // Portfolio weight: this ticker's value / total portfolio value. For simplicity, other
        // holdings use purchase price as a proxy for current price (the dedicated holdings
        // endpoint computes this more accurately with live quotes for every ticker).
        BigDecimal totalPortfolioValue = allLots.stream()
                .map(lot -> {
                    if (lot.name().equalsIgnoreCase(ticker) && currentPrice != null) {
                        return lot.quantity().multiply(currentPrice);
                    }
                    return lot.quantity().multiply(lot.purchasePrice());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal portfolioWeight = totalPortfolioValue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : currentValue.multiply(BigDecimal.valueOf(100)).divide(totalPortfolioValue, MONEY_SCALE, RoundingMode.HALF_UP);

        return new UserPositionDTO(
            quantity, averagePrice, investedValue, currentValue,
            unrealizedGain, unrealizedGainPercent, portfolioWeight
        );
    }
}
