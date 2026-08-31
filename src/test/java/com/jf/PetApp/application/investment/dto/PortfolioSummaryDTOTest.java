package com.jf.PetApp.application.investment.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortfolioSummaryDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        PortfolioSummaryDTO dto = new PortfolioSummaryDTO(
                BigDecimal.valueOf(1000.0), BigDecimal.valueOf(1200.0), BigDecimal.valueOf(200.0), BigDecimal.valueOf(20.0), 5);

        assertEquals(BigDecimal.valueOf(1000.0), dto.investedCapital());
        assertEquals(BigDecimal.valueOf(1200.0), dto.currentValue());
        assertEquals(BigDecimal.valueOf(200.0), dto.totalGain());
        assertEquals(BigDecimal.valueOf(20.0), dto.totalGainPercent());
        assertEquals(5, dto.totalAssets());
    }
}
