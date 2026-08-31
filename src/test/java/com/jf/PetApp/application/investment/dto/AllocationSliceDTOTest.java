package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.InvestmentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AllocationSliceDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        AllocationSliceDTO dto = new AllocationSliceDTO(InvestmentType.STOCKS, BigDecimal.valueOf(1000.0), BigDecimal.valueOf(25.5));

        assertEquals(InvestmentType.STOCKS, dto.type());
        assertEquals(BigDecimal.valueOf(1000.0), dto.currentValue());
        assertEquals(BigDecimal.valueOf(25.5), dto.portfolioPercent());
    }
}
