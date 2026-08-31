package com.jf.PetApp.application.gamification.achievement;

import java.math.BigDecimal;
import java.util.List;

/**
 * The fixed, permanent achievement catalog — the authoritative, server-side
 * mirror of {@code petapp_mobile/lib/features/portfolio/domain/services/achievement_catalog.dart}.
 * Ids, XP rewards and conditions must stay identical to that file; the
 * mobile catalog keeps its own copy only for display metadata (title/
 * description/icon) and for previewing not-yet-saved portfolio changes —
 * this list is what actually grants XP.
 *
 * Per DECISION-014 (XP must never reward investment profit/wealth/portfolio
 * size) and DECISION-027 (XP must never reward investment *activity* either
 * — only learning/practice behavior), every achievement below is kept as a
 * permanent, zero-XP milestone. Achievements remain a real, unlockable
 * badge system; they simply no longer feed the XP/level total.
 */
public final class AchievementCatalog {

    private AchievementCatalog() {
    }

    public static final List<AchievementDefinition> DEFINITIONS = List.of(
            // DECISION-027: XP must never reward investment activity, only
            // learning/practice behavior. Kept as a milestone for flavor,
            // but grants no XP.
            new AchievementDefinition("first_investment", 0, AchievementContext::hasHoldings),
            // DECISION-014: XP must never reward passive-income/wealth signals.
            // Kept as a milestone for flavor, but grants no XP.
            new AchievementDefinition("first_dividend", 0, c -> c.monthlyPassiveIncomeEstimate().compareTo(BigDecimal.ZERO) > 0),
            new AchievementDefinition("positive_return", 0, c -> c.totalGain().compareTo(BigDecimal.ZERO) > 0),
            new AchievementDefinition("portfolio_10k", 0, c -> c.currentValue().compareTo(BigDecimal.valueOf(10_000)) >= 0),
            new AchievementDefinition("portfolio_50k", 0, c -> c.currentValue().compareTo(BigDecimal.valueOf(50_000)) >= 0),
            // DECISION-027: XP must never reward investment activity, only
            // learning/practice behavior. Kept as a milestone for flavor,
            // but grants no XP.
            new AchievementDefinition("diversification_master", 0, c -> c.distinctTypeCount() >= 4),
            new AchievementDefinition("etf_collector", 0, c -> c.distinctFundsTickerCount() >= 3),
            new AchievementDefinition("hundred_days", 0,
                    c -> c.firstPurchaseDate() != null && c.daysSinceFirstPurchase(java.time.LocalDate.now()) >= 100),
            new AchievementDefinition("long_term_investor", 0,
                    c -> c.firstPurchaseDate() != null && c.daysSinceFirstPurchase(java.time.LocalDate.now()) >= 365),
            // DECISION-014: XP must never reward passive-income/wealth signals.
            // Kept as a milestone for flavor, but grants no XP.
            new AchievementDefinition("dividend_hunter", 0, c -> c.annualPassiveIncomeEstimate().compareTo(BigDecimal.valueOf(1_000)) >= 0));
}
