package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import java.math.BigDecimal;

public record AllocationSliceDTO(
    InvestmentType type,
    BigDecimal currentValue,
    BigDecimal portfolioPercent
) {
}
