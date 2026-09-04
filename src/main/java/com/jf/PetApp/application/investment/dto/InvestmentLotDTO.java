package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.InvestmentType;
import com.jf.PetApp.core.domain.enums.PriceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Money and quantity are {@link BigDecimal} — never {@code double} — because
 * this DTO carries the real_portfolio ledger chain achievement thresholds key
 * off: see docs/BACKEND_MODULE_PLAN.md §12.
 *
 * <p>{@code priceStatus} says whether {@code currentPrice} is a real quote or a fallback, so
 * {@code currentValue} is never silently presented as a live valuation — see {@link PriceStatus}.
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
    BigDecimal currentValue,
    PriceStatus priceStatus
) {
}
