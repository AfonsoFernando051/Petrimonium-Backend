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
 * <p>All arithmetic is {@link BigDecimal} at scale 2, {@link RoundingMode#HALF_UP} —
 * matching simulated_portfolio's convention, see docs/BACKEND_MODULE_PLAN.md §12.</p>
 */
@Component
public class UserPositionCalculator {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE);

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
        BigDecimal investedValue = money(tickerLots.stream()
                .map(lot -> lot.quantity().multiply(lot.purchasePrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal averagePrice = quantity.signum() == 0
                ? ZERO
                : investedValue.divide(quantity, MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal price = currentPrice != null ? currentPrice : averagePrice;
        BigDecimal currentValue = money(quantity.multiply(price));
        BigDecimal unrealizedGain = currentValue.subtract(investedValue);
        BigDecimal unrealizedGainPercent = investedValue.signum() == 0
                ? ZERO
                : unrealizedGain.multiply(HUNDRED).divide(investedValue, MONEY_SCALE, RoundingMode.HALF_UP);

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
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal portfolioWeight = totalPortfolioValue.signum() == 0
                ? ZERO
                : currentValue.multiply(HUNDRED).divide(totalPortfolioValue, MONEY_SCALE, RoundingMode.HALF_UP);

        return new UserPositionDTO(
            quantity, averagePrice, investedValue, currentValue,
            unrealizedGain, unrealizedGainPercent, portfolioWeight
        );
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
