package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedPortfolioSummaryDTO;
import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedPositionDTO;
import com.jf.PetApp.application.simulatedportfolio.port.SimulatedPortfolioRepositoryPort;
import com.jf.PetApp.core.domain.SimulatedPortfolio;
import com.jf.PetApp.core.domain.SimulatedPosition;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class GetSimulatedPortfolioUseCaseImpl implements GetSimulatedPortfolioUseCase {

    private final GetOrCreateSimulatedPortfolioUseCase getOrCreateSimulatedPortfolioUseCase;
    private final SimulatedPortfolioRepositoryPort simulatedPortfolioRepository;

    public GetSimulatedPortfolioUseCaseImpl(
            GetOrCreateSimulatedPortfolioUseCase getOrCreateSimulatedPortfolioUseCase,
            SimulatedPortfolioRepositoryPort simulatedPortfolioRepository
    ) {
        this.getOrCreateSimulatedPortfolioUseCase = getOrCreateSimulatedPortfolioUseCase;
        this.simulatedPortfolioRepository = simulatedPortfolioRepository;
    }

    @Override
    public SimulatedPortfolioSummaryDTO execute(String email) {
        SimulatedPortfolio portfolio = getOrCreateSimulatedPortfolioUseCase.execute(email);
        List<SimulatedPosition> positions = simulatedPortfolioRepository.findPositions(portfolio.id());

        BigDecimal totalCostBasis = positions.stream()
                .map(p -> p.quantity().multiply(p.averagePrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SimulatedPositionDTO> positionDTOs = positions.stream()
                .map(p -> toDto(p, totalCostBasis))
                .toList();

        return new SimulatedPortfolioSummaryDTO(
                portfolio.virtualBalance(),
                portfolio.initialBalance(),
                portfolio.currency(),
                portfolio.resetAt(),
                positionDTOs
        );
    }

    private SimulatedPositionDTO toDto(SimulatedPosition position, BigDecimal totalCostBasis) {
        BigDecimal costBasis = position.quantity().multiply(position.averagePrice());
        BigDecimal allocationPercent = totalCostBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : costBasis.multiply(BigDecimal.valueOf(100)).divide(totalCostBasis, 2, RoundingMode.HALF_UP);

        return new SimulatedPositionDTO(
                position.ticker(),
                position.quantity(),
                position.averagePrice(),
                costBasis,
                allocationPercent
        );
    }
}
