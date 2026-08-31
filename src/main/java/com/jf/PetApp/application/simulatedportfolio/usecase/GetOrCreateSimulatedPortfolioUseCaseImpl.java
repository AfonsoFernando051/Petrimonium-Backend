package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.simulatedportfolio.port.SimulatedPortfolioRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.SimulatedPortfolio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * "Create a fictitious wallet" is satisfied lazily: the first time a user's
 * simulated portfolio is requested (dashboard load, order placement, reset),
 * a row is provisioned with the configured starting virtual balance. There
 * is no separate user-facing "create wallet" step to get out of sync with.
 */
@Service
public class GetOrCreateSimulatedPortfolioUseCaseImpl implements GetOrCreateSimulatedPortfolioUseCase {

    private static final String CURRENCY = "BRL";

    private final SimulatedPortfolioRepositoryPort simulatedPortfolioRepository;
    private final UserRepository userRepository;
    private final BigDecimal initialBalance;

    public GetOrCreateSimulatedPortfolioUseCaseImpl(
            SimulatedPortfolioRepositoryPort simulatedPortfolioRepository,
            UserRepository userRepository,
            @Value("${app.simulated-portfolio.initial-balance}") BigDecimal initialBalance
    ) {
        this.simulatedPortfolioRepository = simulatedPortfolioRepository;
        this.userRepository = userRepository;
        this.initialBalance = initialBalance;
    }

    @Override
    public SimulatedPortfolio execute(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));

        return simulatedPortfolioRepository.findByUserEmail(email)
                .orElseGet(() -> simulatedPortfolioRepository.create(email, initialBalance, CURRENCY));
    }
}
