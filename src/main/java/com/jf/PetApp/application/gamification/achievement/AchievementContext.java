package com.jf.PetApp.application.gamification.achievement;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The real portfolio facts an {@link AchievementDefinition} may condition on.
 * Built once per evaluation from the same use cases the Portfolio endpoints
 * already use ({@code GetPortfolioHoldingsUseCase},
 * {@code GetPortfolioSummaryUseCase}, {@code GetPortfolioAllocationUseCase}),
 * so an achievement can never be unlocked on anything the server hasn't
 * independently verified. Money fields are {@link BigDecimal}, matching the
 * real_portfolio calculation chain they're built from.
 */
public record AchievementContext(
        boolean hasHoldings,
        BigDecimal currentValue,
        BigDecimal totalGain,
        int distinctTypeCount,
        int distinctFundsTickerCount,
        LocalDate firstPurchaseDate,
        BigDecimal monthlyPassiveIncomeEstimate,
        BigDecimal annualPassiveIncomeEstimate) {

    public long daysSinceFirstPurchase(LocalDate today) {
        if (firstPurchaseDate == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(firstPurchaseDate, today);
    }
}
