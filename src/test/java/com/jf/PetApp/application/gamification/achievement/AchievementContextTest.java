package com.jf.PetApp.application.gamification.achievement;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AchievementContextTest {

    @Test
    void accessorsReturnConstructedValues() {
        LocalDate firstPurchase = LocalDate.of(2026, 1, 1);
        AchievementContext context = new AchievementContext(
                true, 1000.0, 200.0, 3, 5, firstPurchase, 50.0, 600.0);

        assertEquals(true, context.hasHoldings());
        assertEquals(1000.0, context.currentValue());
        assertEquals(200.0, context.totalGain());
        assertEquals(3, context.distinctTypeCount());
        assertEquals(5, context.distinctFundsTickerCount());
        assertEquals(firstPurchase, context.firstPurchaseDate());
        assertEquals(50.0, context.monthlyPassiveIncomeEstimate());
        assertEquals(600.0, context.annualPassiveIncomeEstimate());
    }

    @Test
    void daysSinceFirstPurchase_WithANullFirstPurchaseDate_ReturnsZero() {
        AchievementContext context = new AchievementContext(
                false, 0.0, 0.0, 0, 0, null, 0.0, 0.0);

        assertEquals(0L, context.daysSinceFirstPurchase(LocalDate.of(2026, 8, 23)));
    }

    @Test
    void daysSinceFirstPurchase_WithAPastFirstPurchaseDate_ReturnsTheDayDifference() {
        AchievementContext context = new AchievementContext(
                true, 100.0, 10.0, 1, 1, LocalDate.of(2026, 8, 1), 0.0, 0.0);

        assertEquals(22L, context.daysSinceFirstPurchase(LocalDate.of(2026, 8, 23)));
    }

    @Test
    void daysSinceFirstPurchase_WithFirstPurchaseDateEqualToToday_ReturnsZero() {
        LocalDate today = LocalDate.of(2026, 8, 23);
        AchievementContext context = new AchievementContext(
                true, 100.0, 10.0, 1, 1, today, 0.0, 0.0);

        assertEquals(0L, context.daysSinceFirstPurchase(today));
    }

    @Test
    void daysSinceFirstPurchase_WithFirstPurchaseDateAfterToday_ReturnsANegativeValue() {
        // Shouldn't happen in practice (future purchase), but the arithmetic must stay consistent.
        AchievementContext context = new AchievementContext(
                true, 100.0, 10.0, 1, 1, LocalDate.of(2026, 8, 25), 0.0, 0.0);

        assertEquals(-2L, context.daysSinceFirstPurchase(LocalDate.of(2026, 8, 23)));
    }
}
