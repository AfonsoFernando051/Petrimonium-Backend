package com.jf.PetApp.application.investment.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortfolioHistoryPointDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        LocalDate date = LocalDate.of(2026, 3, 15);

        PortfolioHistoryPointDTO dto = new PortfolioHistoryPointDTO(date, 1000.0, 1200.0);

        assertEquals(date, dto.date());
        assertEquals(1000.0, dto.investedCapital());
        assertEquals(1200.0, dto.portfolioValue());
    }
}
