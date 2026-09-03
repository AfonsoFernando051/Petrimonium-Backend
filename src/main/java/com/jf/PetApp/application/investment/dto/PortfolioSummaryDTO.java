package com.jf.PetApp.application.investment.dto;

import java.math.BigDecimal;

/**
 * Money fields are {@link BigDecimal} — see docs/BACKEND_MODULE_PLAN.md §12.
 */
public record PortfolioSummaryDTO(
    BigDecimal investedCapital,
    BigDecimal currentValue,
    BigDecimal totalGain,
    BigDecimal totalGainPercent,
    Integer totalAssets
) {
}
