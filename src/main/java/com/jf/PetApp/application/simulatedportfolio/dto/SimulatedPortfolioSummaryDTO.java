package com.jf.PetApp.application.simulatedportfolio.dto;

import java.time.Instant;
import java.util.List;

public record SimulatedPortfolioSummaryDTO(
        String currency,
        Instant resetAt,
        List<SimulatedPositionDTO> positions
) {
}
