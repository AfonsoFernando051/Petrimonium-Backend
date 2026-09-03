package com.jf.PetApp.application.investment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Money fields are {@link BigDecimal} — see docs/BACKEND_MODULE_PLAN.md §12.
 */
public record PortfolioHistoryPointDTO(
    LocalDate date,
    BigDecimal investedCapital,
    BigDecimal portfolioValue
) {
}
