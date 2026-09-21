package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.simulatedportfolio.port.SimulatedPortfolioRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.SimulatedPortfolio;

import org.springframework.stereotype.Service;

/**
 * "Create a fictitious wallet" is satisfied lazily: the first time a user's
 * simulated portfolio is requested (dashboard load, order placement, reset),
 * an empty row is provisioned — no starting balance, no seed positions: the
 * wallet only ever contains what the user registers. There is no separate
 * user-facing "create wallet" step to get out of sync with.
 */
@Service
public class GetOrCreateSimulatedPortfolioUseCaseImpl implements GetOrCreateSimulatedPortfolioUseCase {

    private static final String CURRENCY = "BRL";

    private final SimulatedPortfolioRepositoryPort simulatedPortfolioRepository;
    private final UserRepository userRepository;

    public GetOrCreateSimulatedPortfolioUseCaseImpl(
            SimulatedPortfolioRepositoryPort simulatedPortfolioRepository,
            UserRepository userRepository
    ) {
        this.simulatedPortfolioRepository = simulatedPortfolioRepository;
        this.userRepository = userRepository;
    }

    @Override
    public SimulatedPortfolio execute(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));

        return simulatedPortfolioRepository.findByUserEmail(email)
                .orElseGet(() -> simulatedPortfolioRepository.create(email, CURRENCY));
    }
}
