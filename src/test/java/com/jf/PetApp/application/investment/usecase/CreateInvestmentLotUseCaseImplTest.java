package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.investment.dto.InvestmentDTO;
import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.Investment;
import com.jf.PetApp.core.domain.User;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateInvestmentLotUseCaseImplTest {

    private static final String EMAIL = "investor@test.com";

    @Mock
    private InvestmentRepositoryPort investmentRepositoryPort;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CreateInvestmentLotUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_WhenUserExists_CreatesLotAndReturnsDTO() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new User()));
        InvestmentLotCommand command = new InvestmentLotCommand(
                "PETR4", BigDecimal.valueOf(100), BigDecimal.valueOf(30.5), LocalDate.of(2025, 1, 1), InvestmentType.STOCKS);
        Investment saved = new Investment(7, EMAIL, "PETR4", BigDecimal.valueOf(100), BigDecimal.valueOf(30.5),
                LocalDate.of(2025, 1, 1), InvestmentType.STOCKS);
        when(investmentRepositoryPort.create(eq(EMAIL), any())).thenReturn(saved);

        InvestmentDTO result = useCase.execute(EMAIL, command);

        assertEquals(7, result.id());
        assertEquals("PETR4", result.name());
        assertEquals(BigDecimal.valueOf(100), result.quantity());
        // Every write path today is manual BRL entry — see Investment's javadoc.
        assertEquals("BRL", result.currency());
        assertEquals(AssetOrigin.MANUAL, result.origin());

        ArgumentCaptor<Investment> captor = ArgumentCaptor.forClass(Investment.class);
        verify(investmentRepositoryPort).create(eq(EMAIL), captor.capture());
        assertEquals(EMAIL, captor.getValue().userEmail());
        assertEquals("PETR4", captor.getValue().name());
        assertEquals(null, captor.getValue().id());
        assertEquals("BRL", captor.getValue().currency());
        assertEquals(AssetOrigin.MANUAL, captor.getValue().origin());
    }

    @Test
    void execute_WhenUserDoesNotExist_ThrowsResourceNotFoundAndNeverWrites() {
        String email = "missing@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        InvestmentLotCommand command = new InvestmentLotCommand(
                "PETR4", BigDecimal.ONE, BigDecimal.TEN, LocalDate.now(), InvestmentType.STOCKS);

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(email, command));

        verify(investmentRepositoryPort, never()).create(any(), any());
    }
}
