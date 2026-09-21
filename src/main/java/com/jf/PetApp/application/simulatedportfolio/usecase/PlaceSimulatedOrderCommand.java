package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.core.domain.enums.SimulatedOrderSide;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Application-layer input for {@link PlaceSimulatedOrderUseCase}, mapped by
 * the controller from the HTTP DTO. {@code clientOrderId} may be null — the
 * use case assigns a server-generated one when the client didn't send it,
 * so every order still gets an idempotency key.
 *
 * <p>{@code tradeDate} may be null (or today) for an ordinary live order; a
 * past date backdates the order to that day's historical close — see
 * {@link PlaceSimulatedOrderUseCaseImpl}.
 */
public record PlaceSimulatedOrderCommand(
        String ticker,
        SimulatedOrderSide side,
        BigDecimal quantity,
        String clientOrderId,
        LocalDate tradeDate
) {

    /** A live order, executed now at the current reference price. */
    public PlaceSimulatedOrderCommand(String ticker, SimulatedOrderSide side, BigDecimal quantity, String clientOrderId) {
        this(ticker, side, quantity, clientOrderId, null);
    }
}
