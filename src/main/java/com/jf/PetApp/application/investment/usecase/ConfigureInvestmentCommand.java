package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import java.time.LocalDate;

/**
 * Application-layer input for {@link ConfigureInvestmentsUseCase}, mapped by
 * the controller from the HTTP DTO. Keeps the use case free of any
 * presentation-layer dependency.
 */
public record ConfigureInvestmentCommand(
        String name,
        Double quantity,
        Double purchasePrice,
        LocalDate purchaseDate,
        InvestmentType type
) {
}
