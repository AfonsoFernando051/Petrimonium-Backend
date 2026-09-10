package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Application-layer input for the granular per-lot use cases
 * ({@link CreateInvestmentLotUseCase}, update, delete) — separate from
 * {@link ConfigureInvestmentCommand} on purpose, so the full-replace and
 * granular write paths stay uncoupled even though their fields match today.
 */
public record InvestmentLotCommand(
        String name,
        BigDecimal quantity,
        BigDecimal purchasePrice,
        LocalDate purchaseDate,
        InvestmentType type
) {
}
