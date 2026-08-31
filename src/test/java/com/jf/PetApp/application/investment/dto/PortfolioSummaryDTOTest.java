package com.jf.PetApp.application.investment.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortfolioSummaryDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        PortfolioSummaryDTO dto = new PortfolioSummaryDTO(1000.0, 1200.0, 200.0, 20.0, 5);

        assertEquals(1000.0, dto.investedCapital());
        assertEquals(1200.0, dto.currentValue());
        assertEquals(200.0, dto.totalGain());
        assertEquals(20.0, dto.totalGainPercent());
        assertEquals(5, dto.totalAssets());
    }
}
