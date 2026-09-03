package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.InvestmentType;

public record AllocationSliceDTO(
    InvestmentType type,
    Double currentValue,
    Double portfolioPercent
) {
}
