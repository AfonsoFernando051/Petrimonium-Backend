package com.jf.PetApp.application.gamification.achievement;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AchievementContextTest {

    private static BigDecimal money(double value) {
        return BigDecimal.valueOf(value);
    }

    @Test
    void accessorsReturnConstructedValues() {
        LocalDate firstPurchase = LocalDate.of(2026, 1, 1);
        AchievementContext context = new AchievementContext(
                true, money(1000.0), money(200.0), 3, 5, firstPurchase, money(50.0), money(600.0));

        assertEquals(true, context.hasHoldings());
        assertEquals(money(1000.0), context.currentValue());
        assertEquals(money(200.0), context.totalGain());
        assertEquals(3, context.distinctTypeCount());
        assertEquals(5, context.distinctFundsTickerCount());
        assertEquals(firstPurchase, context.firstPurchaseDate());
        assertEquals(money(50.0), context.monthlyPassiveIncomeEstimate());
        assertEquals(money(600.0), context.annualPassiveIncomeEstimate());
    }

    @Test
    void daysSinceFirstPurchase_WithANullFirstPurchaseDate_ReturnsZero() {
        AchievementContext context = new AchievementContext(
                false, money(0.0), money(0.0), 0, 0, null, money(0.0), money(0.0));

        assertEquals(0L, context.daysSinceFirstPurchase(LocalDate.of(2026, 8, 23)));
    }

    @Test
    void daysSinceFirstPurchase_WithAPastFirstPurchaseDate_ReturnsTheDayDifference() {
        AchievementContext context = new AchievementContext(
                true, money(100.0), money(10.0), 1, 1, LocalDate.of(2026, 8, 1), money(0.0), money(0.0));

        assertEquals(22L, context.daysSinceFirstPurchase(LocalDate.of(2026, 8, 23)));
    }

    @Test
    void daysSinceFirstPurchase_WithFirstPurchaseDateEqualToToday_ReturnsZero() {
        LocalDate today = LocalDate.of(2026, 8, 23);
        AchievementContext context = new AchievementContext(
                true, money(100.0), money(10.0), 1, 1, today, money(0.0), money(0.0));

        assertEquals(0L, context.daysSinceFirstPurchase(today));
    }

    @Test
    void daysSinceFirstPurchase_WithFirstPurchaseDateAfterToday_ReturnsANegativeValue() {
        // Shouldn't happen in practice (future purchase), but the arithmetic must stay consistent.
        AchievementContext context = new AchievementContext(
                true, money(100.0), money(10.0), 1, 1, LocalDate.of(2026, 8, 25), money(0.0), money(0.0));

        assertEquals(-2L, context.daysSinceFirstPurchase(LocalDate.of(2026, 8, 23)));
    }
}
