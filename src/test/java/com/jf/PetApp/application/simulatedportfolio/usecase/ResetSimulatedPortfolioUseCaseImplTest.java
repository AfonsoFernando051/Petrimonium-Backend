package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.simulatedportfolio.port.SimulatedPortfolioRepositoryPort;
import com.jf.PetApp.core.domain.SimulatedPortfolio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResetSimulatedPortfolioUseCaseImplTest {

    @Mock
    private GetOrCreateSimulatedPortfolioUseCase getOrCreateSimulatedPortfolioUseCase;

    @Mock
    private SimulatedPortfolioRepositoryPort simulatedPortfolioRepository;

    private ResetSimulatedPortfolioUseCaseImpl useCase;

    private static final String EMAIL = "learner@test.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new ResetSimulatedPortfolioUseCaseImpl(getOrCreateSimulatedPortfolioUseCase, simulatedPortfolioRepository);
    }

    @Test
    void execute_WithConfirmTrue_ResetsThePortfolio() {
        SimulatedPortfolio portfolio = new SimulatedPortfolio(
                1L, EMAIL, "BRL", null, Instant.now(), Instant.now());
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL)).thenReturn(portfolio);

        useCase.execute(EMAIL, true);

        verify(simulatedPortfolioRepository).resetPortfolio(1L);
    }

    @Test
    void execute_WithConfirmFalse_ThrowsAndNeverResets() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL, false));

        verify(getOrCreateSimulatedPortfolioUseCase, never()).execute(EMAIL);
        verify(simulatedPortfolioRepository, never()).resetPortfolio(org.mockito.ArgumentMatchers.any());
    }
}
