package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.DividendType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DividendRadarEntryDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        LocalDate dataCom = LocalDate.of(2026, 1, 1);
        LocalDate paymentDate = LocalDate.of(2026, 2, 1);
        LocalDate approvedOn = LocalDate.of(2025, 12, 1);

        DividendRadarEntryDTO dto = new DividendRadarEntryDTO(
                "PETR4", DividendType.JCP, "JCP", 1.5, dataCom, paymentDate, approvedOn,
                100.0, 150.0, DividendRadarEntryDTO.STATUS_ANNOUNCED);

        assertEquals("PETR4", dto.ticker());
        assertEquals(DividendType.JCP, dto.type());
        assertEquals(100.0, dto.userQuantity());
        assertEquals(150.0, dto.estimatedGrossAmount());
        assertEquals(DividendRadarEntryDTO.STATUS_ANNOUNCED, dto.status());
    }

    @Test
    void statusConstants_HaveTheExpectedWireValues() {
        assertEquals("ANNOUNCED", DividendRadarEntryDTO.STATUS_ANNOUNCED);
        assertEquals("PAID", DividendRadarEntryDTO.STATUS_PAID);
    }
}
