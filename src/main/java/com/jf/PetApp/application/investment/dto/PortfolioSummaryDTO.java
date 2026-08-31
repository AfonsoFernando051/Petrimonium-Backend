package com.jf.PetApp.application.investment.dto;

import java.math.BigDecimal;

public record PortfolioSummaryDTO(
    BigDecimal investedCapital,
    BigDecimal currentValue,
    BigDecimal totalGain,
    BigDecimal totalGainPercent,
    Integer totalAssets
) {
}
