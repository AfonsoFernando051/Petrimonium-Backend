package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedOrderDTO;
import com.jf.PetApp.application.simulatedportfolio.port.SimulatedPortfolioRepositoryPort;
import com.jf.PetApp.core.domain.SimulatedOrder;
import com.jf.PetApp.core.domain.SimulatedPortfolio;

import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.List;

@Service
public class GetSimulatedOrderHistoryUseCaseImpl implements GetSimulatedOrderHistoryUseCase {

    private final GetOrCreateSimulatedPortfolioUseCase getOrCreateSimulatedPortfolioUseCase;
    private final SimulatedPortfolioRepositoryPort simulatedPortfolioRepository;

    public GetSimulatedOrderHistoryUseCaseImpl(
            GetOrCreateSimulatedPortfolioUseCase getOrCreateSimulatedPortfolioUseCase,
            SimulatedPortfolioRepositoryPort simulatedPortfolioRepository
    ) {
        this.getOrCreateSimulatedPortfolioUseCase = getOrCreateSimulatedPortfolioUseCase;
        this.simulatedPortfolioRepository = simulatedPortfolioRepository;
    }

    @Override
    public List<SimulatedOrderDTO> execute(String email) {
        SimulatedPortfolio portfolio = getOrCreateSimulatedPortfolioUseCase.execute(email);
        return simulatedPortfolioRepository.findOrders(portfolio.id()).stream()
                .map(this::toDto)
                .toList();
    }

    private SimulatedOrderDTO toDto(SimulatedOrder order) {
        return new SimulatedOrderDTO(
                order.id(),
                order.ticker(),
                order.side(),
                order.quantity(),
                order.price(),
                order.price().multiply(order.quantity()).setScale(2, RoundingMode.HALF_UP),
                order.executedAt(),
                order.clientOrderId()
        );
    }
}
