package com.jf.PetApp.infrastructure.controller.investment.dto;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The {@code @Digits} bounds are not arbitrary limits — they are the schema's own precision,
 * stated where a caller can be told about it. V24__investment_precision.sql pinned
 * {@code jf_investments.quantity} to {@code numeric(19,6)} and {@code purchase_price} to
 * {@code numeric(19,2)} (mirrored on {@code InvestmentJpaEntity}'s {@code @Column}), so a wider
 * value is not merely large, it is unstorable: Postgres answers a numeric field overflow, which
 * surfaces to the caller as a 500 for what is plainly a malformed request. {@code @Positive}
 * alone let every such value through to the database to find out.
 *
 * <p>Scale 6 on quantity is what makes fractional shares and crypto work, so the bound has to
 * reject only what the column genuinely cannot hold.
 */
public record AssetRegistrationDto(
        @NotBlank(message = "Asset name is required")
        @Size(max = 255, message = "Asset name must be at most 255 characters")
        String name,
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        @Digits(integer = 13, fraction = 6, message = "Quantity is out of the supported range")
        BigDecimal quantity,
        @NotNull(message = "Purchase price is required")
        @Positive(message = "Purchase price must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "Purchase price is out of the supported range")
        BigDecimal purchasePrice,
        @NotNull(message = "Purchase date is required") LocalDate purchaseDate,
        @NotNull(message = "Asset type is required") InvestmentType type
) {
}
