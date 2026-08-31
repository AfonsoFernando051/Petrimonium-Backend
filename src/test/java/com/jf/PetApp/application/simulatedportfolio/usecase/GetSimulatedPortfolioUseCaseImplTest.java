package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedPortfolioSummaryDTO;
import com.jf.PetApp.application.simulatedportfolio.port.SimulatedPortfolioRepositoryPort;
import com.jf.PetApp.core.domain.SimulatedPortfolio;
import com.jf.PetApp.core.domain.SimulatedPosition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class GetSimulatedPortfolioUseCaseImplTest {

    @Mock
    private GetOrCreateSimulatedPortfolioUseCase getOrCreateSimulatedPortfolioUseCase;

    @Mock
    private SimulatedPortfolioRepositoryPort simulatedPortfolioRepository;

    private GetSimulatedPortfolioUseCaseImpl useCase;

    private static final String EMAIL = "learner@test.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetSimulatedPortfolioUseCaseImpl(getOrCreateSimulatedPortfolioUseCase, simulatedPortfolioRepository);
    }

    @Test
    void execute_WithNoPositions_ReturnsBalanceAndEmptyPositionList() {
        SimulatedPortfolio portfolio = new SimulatedPortfolio(
                1L, EMAIL, new BigDecimal("10000.00"), new BigDecimal("10000.00"), "BRL", null, Instant.now(), Instant.now());
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL)).thenReturn(portfolio);
        when(simulatedPortfolioRepository.findPositions(1L)).thenReturn(List.of());

        SimulatedPortfolioSummaryDTO result = useCase.execute(EMAIL);

        assertEquals(new BigDecimal("10000.00"), result.virtualBalance());
        assertTrue(result.positions().isEmpty());
    }

    @Test
    void execute_WithPositions_ComputesAllocationPercentagesSummingToOneHundred() {
        SimulatedPortfolio portfolio = new SimulatedPortfolio(
                1L, EMAIL, new BigDecimal("4000.00"), new BigDecimal("10000.00"), "BRL", null, Instant.now(), Instant.now());
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL)).thenReturn(portfolio);
        when(simulatedPortfolioRepository.findPositions(1L)).thenReturn(List.of(
                new SimulatedPosition(1L, 1L, "PETR4", new BigDecimal("100"), new BigDecimal("30.00")), // cost 3000
                new SimulatedPosition(2L, 1L, "VALE3", new BigDecimal("100"), new BigDecimal("30.00"))  // cost 3000
        ));

        SimulatedPortfolioSummaryDTO result = useCase.execute(EMAIL);

        assertEquals(2, result.positions().size());
        assertEquals(new BigDecimal("50.00"), result.positions().get(0).allocationPercent());
        assertEquals(new BigDecimal("50.00"), result.positions().get(1).allocationPercent());
    }
}
