package com.jf.PetApp.application.investment.dto;

import java.time.LocalDate;

public record PortfolioHistoryPointDTO(
    LocalDate date,
    Double investedCapital,
    Double portfolioValue
) {
}
