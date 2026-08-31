package com.jf.PetApp.application.simulatedportfolio.dto;

import com.jf.PetApp.core.domain.enums.SimulatedOrderSide;

import java.math.BigDecimal;
import java.time.Instant;

public record SimulatedOrderDTO(
        Long id,
        String ticker,
        SimulatedOrderSide side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal total,
        Instant executedAt,
        String clientOrderId
) {
}
