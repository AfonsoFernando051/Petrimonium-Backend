package com.jf.PetApp.application.investment.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortfolioHistoryPointDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        LocalDate date = LocalDate.of(2026, 3, 15);

        PortfolioHistoryPointDTO dto = new PortfolioHistoryPointDTO(date, BigDecimal.valueOf(1000.0), BigDecimal.valueOf(1200.0));

        assertEquals(date, dto.date());
        assertEquals(BigDecimal.valueOf(1000.0), dto.investedCapital());
        assertEquals(BigDecimal.valueOf(1200.0), dto.portfolioValue());
    }
}
