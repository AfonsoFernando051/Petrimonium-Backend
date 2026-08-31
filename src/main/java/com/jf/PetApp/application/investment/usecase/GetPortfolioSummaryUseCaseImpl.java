package com.jf.PetApp.application.investment.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.jf.PetApp.application.investment.dto.InvestmentLotDTO;
import com.jf.PetApp.application.investment.dto.PortfolioSummaryDTO;

@Service
public class GetPortfolioSummaryUseCaseImpl implements GetPortfolioSummaryUseCase {

    private static final int MONEY_SCALE = 2;

    private final GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase;

    public GetPortfolioSummaryUseCaseImpl(GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase) {
        this.getPortfolioHoldingsUseCase = getPortfolioHoldingsUseCase;
    }

    @Override
    public PortfolioSummaryDTO execute(String email) {
        List<InvestmentLotDTO> lots = getPortfolioHoldingsUseCase.execute(email);

        BigDecimal investedCapital = lots.stream()
                .map(InvestmentLotDTO::investedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal currentValue = lots.stream()
                .map(InvestmentLotDTO::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGain = currentValue.subtract(investedCapital);
        BigDecimal totalGainPercent = investedCapital.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : totalGain.multiply(BigDecimal.valueOf(100)).divide(investedCapital, MONEY_SCALE, RoundingMode.HALF_UP);

        Set<String> distinctTickers = lots.stream().map(InvestmentLotDTO::name).collect(Collectors.toSet());
        Integer totalAssets = distinctTickers.size();

        return new PortfolioSummaryDTO(investedCapital, currentValue, totalGain, totalGainPercent, totalAssets);
    }
}
