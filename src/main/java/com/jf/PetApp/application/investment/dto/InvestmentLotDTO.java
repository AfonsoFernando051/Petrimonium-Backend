package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentLotDTO(
    Integer id,
    String name,
    InvestmentType type,
    BigDecimal quantity,
    BigDecimal purchasePrice,
    LocalDate purchaseDate,
    BigDecimal currentPrice,
    BigDecimal investedValue,
    BigDecimal currentValue
) {
}
