package com.jf.PetApp.infrastructure.controller.simulatedportfolio.dto;

import com.jf.PetApp.core.domain.enums.SimulatedOrderSide;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlaceSimulatedOrderRequest(
        @NotBlank String ticker,
        @NotNull SimulatedOrderSide side,
        @NotNull @DecimalMin(value = "0.000001", message = "quantity must be greater than zero") BigDecimal quantity,
        String clientOrderId
) {
}
