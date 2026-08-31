package com.jf.PetApp.application.investment.dto;

import java.math.BigDecimal;

/**
 * The authenticated user's aggregated position in a specific asset —
 * computed from their real {@code jf_investments} lots and the current
 * market price. Every field traces back to validated data; nothing is
 * estimated or fabricated.
 */
public record UserPositionDTO(
    BigDecimal quantity,
    BigDecimal averagePrice,
    BigDecimal investedValue,
    BigDecimal currentValue,
    BigDecimal unrealizedGain,
    BigDecimal unrealizedGainPercent,
    BigDecimal portfolioWeight
) {
}
