package com.jf.PetApp.application.gamification.achievement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * DECISION-014 + DECISION-027 regression coverage. DECISION-014: XP must
 * never be awarded for portfolio wealth, profit, or passive-income signals.
 * DECISION-027: XP must never be awarded for investment *activity* either
 * (diversifying, holding for a duration, collecting distinct assets) — only
 * for learning/practice behavior. Together these zero out every achievement
 * in this portfolio-derived catalog; achievements remain real, permanent,
 * unlockable milestones, they simply no longer feed the XP/level total.
 */
class AchievementCatalogTest {

    private static final Set<String> WEALTH_OR_PROFIT_DERIVED_CODES = Set.of(
            "positive_return", "portfolio_10k", "portfolio_50k", "first_dividend", "dividend_hunter");

    private static final Set<String> INVESTMENT_ACTIVITY_DERIVED_CODES = Set.of(
            "first_investment", "diversification_master", "etf_collector", "hundred_days", "long_term_investor");

    @Test
    void wealthProfitAndIncomeDerivedAchievements_GrantZeroXp() {
        for (AchievementDefinition definition : AchievementCatalog.DEFINITIONS) {
            if (WEALTH_OR_PROFIT_DERIVED_CODES.contains(definition.code())) {
                assertEquals(0, definition.xpReward(),
                        "Achievement '" + definition.code() + "' is wealth/profit/income-derived and must grant 0 XP");
            }
        }
    }

    @Test
    void investmentActivityDerivedAchievements_GrantZeroXp() {
        for (AchievementDefinition definition : AchievementCatalog.DEFINITIONS) {
            if (INVESTMENT_ACTIVITY_DERIVED_CODES.contains(definition.code())) {
                assertEquals(0, definition.xpReward(),
                        "Achievement '" + definition.code() + "' is investment-activity-derived and must grant 0 XP (DECISION-027)");
            }
        }
    }

    @Test
    void everyAchievementInTheCatalogIsZeroXp() {
        for (AchievementDefinition definition : AchievementCatalog.DEFINITIONS) {
            assertEquals(0, definition.xpReward(),
                    "Achievement '" + definition.code() + "' must grant 0 XP — the catalog is entirely "
                            + "portfolio-derived, and XP may only come from learning/practice behavior");
        }
    }

    @Test
    void everyWealthOrProfitDerivedCodeIsStillPresentInTheCatalog() {
        Set<String> catalogCodes = AchievementCatalog.DEFINITIONS.stream()
                .map(AchievementDefinition::code)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(catalogCodes.containsAll(WEALTH_OR_PROFIT_DERIVED_CODES),
                "Expected all known wealth/profit/income-derived achievement codes to still exist in the catalog");
        assertTrue(catalogCodes.containsAll(INVESTMENT_ACTIVITY_DERIVED_CODES),
                "Expected all known investment-activity-derived achievement codes to still exist in the catalog");
    }

    @Test
    void maximumAttainableXp_IsZero_RegardlessOfWealthOrActivity() {
        AchievementContext maxedOutContext = new AchievementContext(
                true, 1_000_000.0, 1_000_000.0, 10, 10, LocalDate.of(2000, 1, 1), 100_000.0, 100_000.0);

        int totalXp = AchievementCatalog.DEFINITIONS.stream()
                .filter(definition -> definition.qualifies().test(maxedOutContext))
                .mapToInt(AchievementDefinition::xpReward)
                .sum();

        assertEquals(0, totalXp,
                "A user maxing out every achievement condition (wealth or activity) should never earn XP from it");
    }
}
