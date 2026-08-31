package com.jf.PetApp.application.simulatedportfolio.dto;

import java.math.BigDecimal;

public record SimulatedPositionDTO(
        String ticker,
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal costBasis,
        BigDecimal allocationPercent
) {
}
