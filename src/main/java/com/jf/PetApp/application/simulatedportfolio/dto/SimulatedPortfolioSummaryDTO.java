package com.jf.PetApp.application.simulatedportfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SimulatedPortfolioSummaryDTO(
        BigDecimal virtualBalance,
        BigDecimal initialBalance,
        String currency,
        Instant resetAt,
        List<SimulatedPositionDTO> positions
) {
}
