package com.jf.PetApp.application.investment.dto;

/**
 * The authenticated user's aggregated position in a specific asset —
 * computed from their real {@code jf_investments} lots and the current
 * market price. Every field traces back to validated data; nothing is
 * estimated or fabricated.
 */
public record UserPositionDTO(
    double quantity,
    double averagePrice,
    double investedValue,
    double currentValue,
    double unrealizedGain,
    double unrealizedGainPercent,
    double portfolioWeight
) {
}
