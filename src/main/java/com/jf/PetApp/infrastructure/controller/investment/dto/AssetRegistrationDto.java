package com.jf.PetApp.infrastructure.controller.investment.dto;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record AssetRegistrationDto(
        @NotBlank(message = "Asset name is required") String name,
        @NotNull(message = "Quantity is required") @Positive(message = "Quantity must be greater than zero") Double quantity,
        @NotNull(message = "Purchase price is required") @Positive(message = "Purchase price must be greater than zero") Double purchasePrice,
        @NotNull(message = "Purchase date is required") LocalDate purchaseDate,
        @NotNull(message = "Asset type is required") InvestmentType type
) {
}
