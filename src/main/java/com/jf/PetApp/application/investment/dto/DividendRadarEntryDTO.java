package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.DividendType;

import java.time.LocalDate;

/**
 * A {@link DividendDTO} enriched with the requesting user's real held
 * quantity for that ticker — the unit the Dividend Radar screen renders.
 * {@code estimatedGrossAmount} is a straight multiplication of confirmed
 * provider data by the user's real position; it is never itself a value
 * fabricated or estimated from an assumed yield (contrast with
 * {@code PassiveIncomeEstimate} on the mobile client, which is).
 */
public record DividendRadarEntryDTO(
    String ticker,
    DividendType type,
    String rawLabel,
    Double ratePerShare,
    LocalDate dataCom,
    LocalDate paymentDate,
    LocalDate approvedOn,
    Double userQuantity,
    Double estimatedGrossAmount,
    String status
) {
    public static final String STATUS_ANNOUNCED = "ANNOUNCED";
    public static final String STATUS_PAID = "PAID";
}
