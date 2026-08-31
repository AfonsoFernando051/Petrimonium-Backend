package com.jf.PetApp.application.investment.usecase;

import java.util.List;

import com.jf.PetApp.application.investment.dto.PortfolioHistoryPointDTO;

public interface GetPortfolioHistoryUseCase {
    List<PortfolioHistoryPointDTO> execute(String email, String range);
}
