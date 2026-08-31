package com.jf.PetApp.application.investment.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserPositionDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        UserPositionDTO dto = new UserPositionDTO(10.0, 20.0, 200.0, 250.0, 50.0, 25.0, 0.4);

        assertEquals(10.0, dto.quantity());
        assertEquals(20.0, dto.averagePrice());
        assertEquals(200.0, dto.investedValue());
        assertEquals(250.0, dto.currentValue());
        assertEquals(50.0, dto.unrealizedGain());
        assertEquals(25.0, dto.unrealizedGainPercent());
        assertEquals(0.4, dto.portfolioWeight());
    }
}
