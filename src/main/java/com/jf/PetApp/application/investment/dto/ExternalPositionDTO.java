package com.jf.PetApp.application.investment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A real position as reported by an external brokerage/custody provider
 * (e.g. B3), in a shape independent of that provider's own wire format —
 * every {@link com.jf.PetApp.application.investment.port.RealPortfolioSyncPort}
 * implementation must map its provider's response onto this DTO, never
 * expose provider-specific fields upstream.
 */
public record ExternalPositionDTO(
        String ticker,
        BigDecimal quantity,
        BigDecimal averagePrice,
        LocalDate asOf
) {
}
