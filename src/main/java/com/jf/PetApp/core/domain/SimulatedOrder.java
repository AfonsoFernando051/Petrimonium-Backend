package com.jf.PetApp.core.domain;

import com.jf.PetApp.core.domain.enums.SimulatedOrderSide;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One executed simulated buy/sell — append-only, never edited or deleted.
 * {@code clientOrderId} is always present (client-supplied, or a
 * server-generated UUID when the client didn't send one) and unique per
 * portfolio, making a retried place-order request idempotent — see
 * PlaceSimulatedOrderUseCaseImpl.
 */
public record SimulatedOrder(
        Long id,
        Long portfolioId,
        String ticker,
        SimulatedOrderSide side,
        BigDecimal quantity,
        BigDecimal price,
        Instant executedAt,
        String clientOrderId
) {
}
