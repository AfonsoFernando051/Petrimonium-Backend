package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.core.domain.SimulatedPortfolio;

public interface GetOrCreateSimulatedPortfolioUseCase {
    SimulatedPortfolio execute(String email);
}
