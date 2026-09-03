package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Money and quantity are {@link BigDecimal} — never {@code double} — because
 * this DTO carries the real_portfolio ledger chain achievement thresholds key
 * off: see docs/BACKEND_MODULE_PLAN.md §12.
 */
public record InvestmentLotDTO(
    Integer id,
    String name,
    InvestmentType type,
    BigDecimal quantity,
    BigDecimal purchasePrice,
    LocalDate purchaseDate,
    BigDecimal currentPrice,
    BigDecimal investedValue,
    BigDecimal currentValue
) {
}
