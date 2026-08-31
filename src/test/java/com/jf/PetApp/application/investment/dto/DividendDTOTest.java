package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.DividendType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DividendDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        LocalDate dataCom = LocalDate.of(2026, 1, 1);
        LocalDate paymentDate = LocalDate.of(2026, 2, 1);
        LocalDate approvedOn = LocalDate.of(2025, 12, 1);

        DividendDTO dto = new DividendDTO("PETR4", DividendType.DIVIDENDO, "Dividendo", 1.5, dataCom, paymentDate, approvedOn);

        assertEquals("PETR4", dto.ticker());
        assertEquals(DividendType.DIVIDENDO, dto.type());
        assertEquals("Dividendo", dto.rawLabel());
        assertEquals(1.5, dto.ratePerShare());
        assertEquals(dataCom, dto.dataCom());
        assertEquals(paymentDate, dto.paymentDate());
        assertEquals(approvedOn, dto.approvedOn());
    }
}
