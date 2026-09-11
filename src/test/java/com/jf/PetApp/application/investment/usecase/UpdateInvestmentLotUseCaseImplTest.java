package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.investment.dto.InvestmentDTO;
import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import com.jf.PetApp.core.domain.Investment;
import com.jf.PetApp.core.domain.enums.AssetOrigin;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateInvestmentLotUseCaseImplTest {

    private static final String EMAIL = "investor@test.com";

    @Mock
    private InvestmentRepositoryPort investmentRepositoryPort;

    @InjectMocks
    private UpdateInvestmentLotUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_DelegatesToScopedPortUpdate_AndReturnsDTO() {
        InvestmentLotCommand command = new InvestmentLotCommand(
                "PETR4", BigDecimal.valueOf(150), BigDecimal.valueOf(31.0), LocalDate.of(2025, 2, 1), InvestmentType.STOCKS);
        // The port's own update() preserves whatever currency/origin the existing row already had
        // (an edit never touches those) — a non-default value here proves toDTO() passes through
        // whatever the port returns instead of re-stamping BRL/MANUAL itself.
        Investment updated = new Investment(7, EMAIL, "PETR4", BigDecimal.valueOf(150), BigDecimal.valueOf(31.0),
                LocalDate.of(2025, 2, 1), InvestmentType.STOCKS, "USD", AssetOrigin.BROKER_SYNC);
        when(investmentRepositoryPort.update(eq(7), eq(EMAIL), org.mockito.ArgumentMatchers.any())).thenReturn(updated);

        InvestmentDTO result = useCase.execute(EMAIL, 7, command);

        assertEquals(7, result.id());
        assertEquals(BigDecimal.valueOf(150), result.quantity());
        assertEquals("USD", result.currency());
        assertEquals(AssetOrigin.BROKER_SYNC, result.origin());

        ArgumentCaptor<Investment> captor = ArgumentCaptor.forClass(Investment.class);
        verify(investmentRepositoryPort).update(eq(7), eq(EMAIL), captor.capture());
        assertEquals("PETR4", captor.getValue().name());
    }

    @Test
    void execute_WhenPortRejectsAsNotOwned_PropagatesResourceNotFound() {
        InvestmentLotCommand command = new InvestmentLotCommand(
                "PETR4", BigDecimal.ONE, BigDecimal.TEN, LocalDate.now(), InvestmentType.STOCKS);
        when(investmentRepositoryPort.update(eq(7), eq(EMAIL), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ResourceNotFoundException("Investment not found: 7"));

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(EMAIL, 7, command));
    }
}
