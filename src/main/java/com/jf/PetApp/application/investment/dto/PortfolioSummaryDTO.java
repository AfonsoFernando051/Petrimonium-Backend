package com.jf.PetApp.application.investment.dto;

public record PortfolioSummaryDTO(
    Double investedCapital,
    Double currentValue,
    Double totalGain,
    Double totalGainPercent,
    Integer totalAssets
) {
}
