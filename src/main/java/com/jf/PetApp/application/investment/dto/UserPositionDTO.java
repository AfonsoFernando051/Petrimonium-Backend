package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.PriceStatus;

import java.math.BigDecimal;

/**
 * The authenticated user's aggregated position in a specific asset —
 * computed from their real {@code jf_investments} lots and the current
 * market price. Every field traces back to validated data; nothing is
 * estimated or fabricated.
 *
 * <p>Money and quantity are {@link BigDecimal} — see
 * docs/BACKEND_MODULE_PLAN.md §12.</p>
 *
 * <p>{@code priceStatus} carries the same provenance flag
 * {@code GetPortfolioHoldingsUseCaseImpl} already attaches to holdings —
 * without it, a stale/missing quote is indistinguishable downstream from a
 * real quote that happens to equal the purchase price.</p>
 */
public record UserPositionDTO(
    BigDecimal quantity,
    BigDecimal averagePrice,
    BigDecimal investedValue,
    BigDecimal currentValue,
    BigDecimal unrealizedGain,
    BigDecimal unrealizedGainPercent,
    BigDecimal portfolioWeight,
    PriceStatus priceStatus
) {
}
