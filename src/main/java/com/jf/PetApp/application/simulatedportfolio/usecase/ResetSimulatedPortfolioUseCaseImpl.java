package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.simulatedportfolio.port.SimulatedPortfolioRepositoryPort;
import com.jf.PetApp.core.domain.SimulatedPortfolio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wipes every simulated position and order and restores the portfolio's
 * original starting balance — "reiniciar a simulação mediante confirmação".
 * {@code confirm} is validated here too (defense in depth beyond the
 * controller's {@code @AssertTrue}), since this is a destructive operation.
 */
@Service
public class ResetSimulatedPortfolioUseCaseImpl implements ResetSimulatedPortfolioUseCase {

    private final GetOrCreateSimulatedPortfolioUseCase getOrCreateSimulatedPortfolioUseCase;
    private final SimulatedPortfolioRepositoryPort simulatedPortfolioRepository;

    public ResetSimulatedPortfolioUseCaseImpl(
            GetOrCreateSimulatedPortfolioUseCase getOrCreateSimulatedPortfolioUseCase,
            SimulatedPortfolioRepositoryPort simulatedPortfolioRepository
    ) {
        this.getOrCreateSimulatedPortfolioUseCase = getOrCreateSimulatedPortfolioUseCase;
        this.simulatedPortfolioRepository = simulatedPortfolioRepository;
    }

    @Override
    @Transactional
    public void execute(String email, boolean confirm) {
        if (!confirm) {
            throw new IllegalArgumentException("Resetting the simulated portfolio requires explicit confirmation");
        }

        SimulatedPortfolio portfolio = getOrCreateSimulatedPortfolioUseCase.execute(email);
        simulatedPortfolioRepository.resetPortfolio(portfolio.id(), portfolio.initialBalance());
    }
}
