package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedOrderDTO;
import com.jf.PetApp.application.simulatedportfolio.port.SimulatedPortfolioRepositoryPort;
import com.jf.PetApp.core.domain.SimulatedOrder;
import com.jf.PetApp.core.domain.SimulatedPortfolio;
import com.jf.PetApp.core.domain.enums.SimulatedOrderSide;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class GetSimulatedOrderHistoryUseCaseImplTest {

    @Mock
    private GetOrCreateSimulatedPortfolioUseCase getOrCreateSimulatedPortfolioUseCase;

    @Mock
    private SimulatedPortfolioRepositoryPort simulatedPortfolioRepository;

    private GetSimulatedOrderHistoryUseCaseImpl useCase;

    private static final String EMAIL = "learner@test.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetSimulatedOrderHistoryUseCaseImpl(getOrCreateSimulatedPortfolioUseCase, simulatedPortfolioRepository);
    }

    @Test
    void execute_MapsEachOrderIncludingComputedTotal() {
        SimulatedPortfolio portfolio = new SimulatedPortfolio(
                1L, EMAIL, new BigDecimal("10000.00"), new BigDecimal("10000.00"), "BRL", null, Instant.now(), Instant.now());
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL)).thenReturn(portfolio);
        when(simulatedPortfolioRepository.findOrders(1L)).thenReturn(List.of(
                new SimulatedOrder(1L, 1L, "PETR4", SimulatedOrderSide.BUY,
                        new BigDecimal("10"), new BigDecimal("30.50"), Instant.now(), "order-1")
        ));

        List<SimulatedOrderDTO> result = useCase.execute(EMAIL);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("305.00"), result.get(0).total());
    }
}
