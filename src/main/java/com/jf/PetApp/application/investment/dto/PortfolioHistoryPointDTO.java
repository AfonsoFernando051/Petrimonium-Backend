package com.jf.PetApp.application.investment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioHistoryPointDTO(
    LocalDate date,
    BigDecimal investedCapital,
    BigDecimal portfolioValue
) {
}
