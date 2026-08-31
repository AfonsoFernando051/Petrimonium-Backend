package com.jf.PetApp.application.investment.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserPositionDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        UserPositionDTO dto = new UserPositionDTO(
                BigDecimal.valueOf(10.0), BigDecimal.valueOf(20.0), BigDecimal.valueOf(200.0), BigDecimal.valueOf(250.0),
                BigDecimal.valueOf(50.0), BigDecimal.valueOf(25.0), BigDecimal.valueOf(0.4));

        assertEquals(BigDecimal.valueOf(10.0), dto.quantity());
        assertEquals(BigDecimal.valueOf(20.0), dto.averagePrice());
        assertEquals(BigDecimal.valueOf(200.0), dto.investedValue());
        assertEquals(BigDecimal.valueOf(250.0), dto.currentValue());
        assertEquals(BigDecimal.valueOf(50.0), dto.unrealizedGain());
        assertEquals(BigDecimal.valueOf(25.0), dto.unrealizedGainPercent());
        assertEquals(BigDecimal.valueOf(0.4), dto.portfolioWeight());
    }
}
