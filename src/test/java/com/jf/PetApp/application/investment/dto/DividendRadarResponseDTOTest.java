package com.jf.PetApp.application.investment.dto;

import com.jf.PetApp.core.domain.enums.DividendType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DividendRadarResponseDTOTest {

    @Test
    void accessorsReturnConstructedValues() {
        DividendRadarEntryDTO entry = new DividendRadarEntryDTO(
                "PETR4", DividendType.DIVIDENDO, "Dividendo", 1.5,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), LocalDate.of(2025, 12, 1),
                10.0, 15.0, DividendRadarEntryDTO.STATUS_PAID);

        DividendRadarResponseDTO dto = new DividendRadarResponseDTO(List.of(), List.of(entry));

        assertEquals(List.of(), dto.upcoming());
        assertEquals(List.of(entry), dto.history());
    }
}
