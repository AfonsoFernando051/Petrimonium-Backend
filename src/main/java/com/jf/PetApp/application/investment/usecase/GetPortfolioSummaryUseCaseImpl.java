package com.jf.PetApp.application.investment.usecase;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.jf.PetApp.application.investment.dto.InvestmentLotDTO;
import com.jf.PetApp.application.investment.dto.PortfolioSummaryDTO;

@Service
public class GetPortfolioSummaryUseCaseImpl implements GetPortfolioSummaryUseCase {

    private final GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase;

    public GetPortfolioSummaryUseCaseImpl(GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase) {
        this.getPortfolioHoldingsUseCase = getPortfolioHoldingsUseCase;
    }

    @Override
    public PortfolioSummaryDTO execute(String email) {
        List<InvestmentLotDTO> lots = getPortfolioHoldingsUseCase.execute(email);

        Double investedCapital = lots.stream().mapToDouble(InvestmentLotDTO::investedValue).sum();
        Double currentValue = lots.stream().mapToDouble(InvestmentLotDTO::currentValue).sum();
        Double totalGain = currentValue - investedCapital;
        Double totalGainPercent = investedCapital == 0.0 ? 0.0 : (totalGain / investedCapital) * 100;

        Set<String> distinctTickers = lots.stream().map(InvestmentLotDTO::name).collect(Collectors.toSet());
        Integer totalAssets = distinctTickers.size();

        return new PortfolioSummaryDTO(investedCapital, currentValue, totalGain, totalGainPercent, totalAssets);
    }
}
