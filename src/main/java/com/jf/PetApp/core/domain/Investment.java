package com.jf.PetApp.core.domain;

import com.jf.PetApp.core.domain.enums.InvestmentType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single investment lot, independent of how it's persisted. {@code id} is
 * {@code null} for a lot that hasn't been saved yet (e.g. built from a
 * registration command before the adapter assigns a database id).
 *
 * <p>{@code quantity}/{@code purchasePrice} are {@link BigDecimal} — never
 * {@code double} — since this is real money: see
 * docs/BACKEND_MODULE_PLAN.md §12 for the precision/scale convention
 * (quantity scale 6 for fractional shares, price scale 2).</p>
 */
public record Investment(
        Integer id,
        String userEmail,
        String name,
        BigDecimal quantity,
        BigDecimal purchasePrice,
        LocalDate purchaseDate,
        InvestmentType type
) {
}
