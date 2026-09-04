package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.InvestmentType;
import com.jf.PetApp.core.domain.enums.PriceStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvestmentLotDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        LocalDate purchaseDate = LocalDate.of(2025, 6, 1);

        InvestmentLotDTO dto = new InvestmentLotDTO(
                1, "Petrobras", InvestmentType.STOCKS, BigDecimal.valueOf(10.0), BigDecimal.valueOf(30.0), purchaseDate,
                BigDecimal.valueOf(35.0), BigDecimal.valueOf(300.0), BigDecimal.valueOf(350.0), PriceStatus.LIVE);

        assertEquals(1, dto.id());
        assertEquals("Petrobras", dto.name());
        assertEquals(InvestmentType.STOCKS, dto.type());
        assertEquals(BigDecimal.valueOf(10.0), dto.quantity());
        assertEquals(BigDecimal.valueOf(30.0), dto.purchasePrice());
        assertEquals(purchaseDate, dto.purchaseDate());
        assertEquals(BigDecimal.valueOf(35.0), dto.currentPrice());
        assertEquals(BigDecimal.valueOf(300.0), dto.investedValue());
        assertEquals(BigDecimal.valueOf(350.0), dto.currentValue());
    }
}
