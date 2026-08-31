package com.jf.PetApp.core.domain;

import java.math.BigDecimal;

/**
 * A simulated holding of {@code ticker} within one {@link SimulatedPortfolio}.
 * Fully closing a position (selling the entire quantity) removes its row
 * rather than leaving a zero-quantity one — see
 * PlaceSimulatedOrderUseCaseImpl.
 */
public record SimulatedPosition(
        Long id,
        Long portfolioId,
        String ticker,
        BigDecimal quantity,
        BigDecimal averagePrice
) {
}
