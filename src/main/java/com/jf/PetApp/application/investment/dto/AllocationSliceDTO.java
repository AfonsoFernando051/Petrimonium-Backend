package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import java.math.BigDecimal;

/**
 * Money fields are {@link BigDecimal} — see docs/BACKEND_MODULE_PLAN.md §12.
 */
public record AllocationSliceDTO(
    InvestmentType type,
    BigDecimal currentValue,
    BigDecimal portfolioPercent
) {
}
