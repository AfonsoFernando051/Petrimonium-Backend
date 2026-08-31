package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.InvestmentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AllocationSliceDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        AllocationSliceDTO dto = new AllocationSliceDTO(InvestmentType.STOCKS, 1000.0, 25.5);

        assertEquals(InvestmentType.STOCKS, dto.type());
        assertEquals(1000.0, dto.currentValue());
        assertEquals(25.5, dto.portfolioPercent());
    }
}
