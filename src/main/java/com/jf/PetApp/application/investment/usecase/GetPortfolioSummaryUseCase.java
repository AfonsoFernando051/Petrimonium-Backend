package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.PortfolioSummaryDTO;

public interface GetPortfolioSummaryUseCase {
    PortfolioSummaryDTO execute(String email);
}
