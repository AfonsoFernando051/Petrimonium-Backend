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
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase;

    public GetPortfolioSummaryUseCaseImpl(GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase) {
        this.getPortfolioHoldingsUseCase = getPortfolioHoldingsUseCase;
    }

    @Override
    public PortfolioSummaryDTO execute(String email) {
        List<InvestmentLotDTO> lots = getPortfolioHoldingsUseCase.execute(email);

        BigDecimal investedCapital = sum(lots, InvestmentLotDTO::investedValue);
        BigDecimal currentValue = sum(lots, InvestmentLotDTO::currentValue);
        BigDecimal totalGain = currentValue.subtract(investedCapital);
        BigDecimal totalGainPercent = investedCapital.signum() == 0
                ? BigDecimal.ZERO.setScale(MONEY_SCALE)
                : totalGain.multiply(HUNDRED).divide(investedCapital, MONEY_SCALE, RoundingMode.HALF_UP);

        Set<String> distinctTickers = lots.stream().map(InvestmentLotDTO::name).collect(Collectors.toSet());
        Integer totalAssets = distinctTickers.size();

        return new PortfolioSummaryDTO(investedCapital, currentValue, totalGain, totalGainPercent, totalAssets);
    }

    private static BigDecimal sum(List<InvestmentLotDTO> lots,
                                  java.util.function.Function<InvestmentLotDTO, BigDecimal> field) {
        return lots.stream()
                .map(field)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
