package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Application-layer input for {@link ConfigureInvestmentsUseCase}, mapped by
 * the controller from the HTTP DTO. Keeps the use case free of any
 * presentation-layer dependency.
 */
public record ConfigureInvestmentCommand(
        String name,
        BigDecimal quantity,
        BigDecimal purchasePrice,
        LocalDate purchaseDate,
        InvestmentType type
) {
}
