package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.core.domain.enums.SimulatedOrderSide;

import java.math.BigDecimal;

/**
 * Application-layer input for {@link PlaceSimulatedOrderUseCase}, mapped by
 * the controller from the HTTP DTO. {@code clientOrderId} may be null — the
 * use case assigns a server-generated one when the client didn't send it,
 * so every order still gets an idempotency key.
 */
public record PlaceSimulatedOrderCommand(
        String ticker,
        SimulatedOrderSide side,
        BigDecimal quantity,
        String clientOrderId
) {
}
