package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class DeleteInvestmentLotUseCaseImplTest {

    private static final String EMAIL = "investor@test.com";

    @Mock
    private InvestmentRepositoryPort investmentRepositoryPort;

    @InjectMocks
    private DeleteInvestmentLotUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_DelegatesToScopedPortDelete() {
        useCase.execute(EMAIL, 7);

        verify(investmentRepositoryPort).delete(eq(7), eq(EMAIL));
    }

    @Test
    void execute_WhenPortRejectsAsNotOwned_PropagatesResourceNotFound() {
        doThrow(new ResourceNotFoundException("Investment not found: 7"))
                .when(investmentRepositoryPort).delete(eq(7), eq(EMAIL));

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(EMAIL, 7));
    }
}
