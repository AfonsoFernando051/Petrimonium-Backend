package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.InvestmentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvestmentLotDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        LocalDate purchaseDate = LocalDate.of(2025, 6, 1);

        InvestmentLotDTO dto = new InvestmentLotDTO(
                1, "Petrobras", InvestmentType.STOCKS, 10.0, 30.0, purchaseDate, 35.0, 300.0, 350.0);

        assertEquals(1, dto.id());
        assertEquals("Petrobras", dto.name());
        assertEquals(InvestmentType.STOCKS, dto.type());
        assertEquals(10.0, dto.quantity());
        assertEquals(30.0, dto.purchasePrice());
        assertEquals(purchaseDate, dto.purchaseDate());
        assertEquals(35.0, dto.currentPrice());
        assertEquals(300.0, dto.investedValue());
        assertEquals(350.0, dto.currentValue());
    }
}
