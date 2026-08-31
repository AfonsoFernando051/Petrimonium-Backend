package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedPortfolioSummaryDTO;

public interface GetSimulatedPortfolioUseCase {
    SimulatedPortfolioSummaryDTO execute(String email);
}
