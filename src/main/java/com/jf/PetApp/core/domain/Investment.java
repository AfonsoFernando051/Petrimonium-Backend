package com.jf.PetApp.core.domain;

import com.jf.PetApp.core.domain.enums.InvestmentType;
import java.time.LocalDate;

/**
 * A single investment lot, independent of how it's persisted. {@code id} is
 * {@code null} for a lot that hasn't been saved yet (e.g. built from a
 * registration command before the adapter assigns a database id).
 */
public record Investment(
        Integer id,
        String userEmail,
        String name,
        Double quantity,
        Double purchasePrice,
        LocalDate purchaseDate,
        InvestmentType type
) {
}
