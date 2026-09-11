package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.AssetOrigin;
import com.jf.PetApp.core.domain.enums.InvestmentType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response of a granular write (create/update) on a single lot. Deliberately
 * without {@code currentPrice}/{@code investedValue}/{@code currentValue} —
 * those require {@link com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort}
 * (see {@code GetPortfolioHoldingsUseCaseImpl}), and mixing quoting into a
 * write use case would duplicate that enrichment. Callers refetch
 * {@code GET /api/investments} for the priced view, same as today.
 */
public record InvestmentDTO(
        Integer id,
        String name,
        BigDecimal quantity,
        BigDecimal purchasePrice,
        LocalDate purchaseDate,
        InvestmentType type,
        String currency,
        AssetOrigin origin
) {
}
