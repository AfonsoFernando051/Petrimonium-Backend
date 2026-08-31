package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.DividendType;

import java.time.LocalDate;

/**
 * A single confirmed corporate-action payment for a ticker, exactly as
 * reported by the external market data provider. Pure market data — no
 * user-specific fields (quantity/estimated amount are computed one layer up,
 * in {@link DividendRadarEntryDTO}, once holdings are known).
 *
 * {@code dataCom} is the provider's "lastDatePrior" — the last date a
 * shareholder could hold the asset and still qualify for this payment.
 */
public record DividendDTO(
    String ticker,
    DividendType type,
    String rawLabel,
    Double ratePerShare,
    LocalDate dataCom,
    LocalDate paymentDate,
    LocalDate approvedOn
) {
}
