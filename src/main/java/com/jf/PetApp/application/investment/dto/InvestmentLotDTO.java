package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import java.time.LocalDate;

public record InvestmentLotDTO(
    Integer id,
    String name,
    InvestmentType type,
    Double quantity,
    Double purchasePrice,
    LocalDate purchaseDate,
    Double currentPrice,
    Double investedValue,
    Double currentValue
) {
}
