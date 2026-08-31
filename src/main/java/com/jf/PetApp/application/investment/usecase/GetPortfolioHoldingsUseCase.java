package com.jf.PetApp.application.investment.usecase;

import java.util.List;

import com.jf.PetApp.application.investment.dto.InvestmentLotDTO;

public interface GetPortfolioHoldingsUseCase {
    List<InvestmentLotDTO> execute(String email);
}
