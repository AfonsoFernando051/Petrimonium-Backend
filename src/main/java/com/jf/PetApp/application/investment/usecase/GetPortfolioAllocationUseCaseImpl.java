package com.jf.PetApp.application.investment.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.jf.PetApp.application.investment.dto.AllocationSliceDTO;
import com.jf.PetApp.application.investment.dto.InvestmentLotDTO;
import com.jf.PetApp.core.domain.enums.InvestmentType;

@Service
public class GetPortfolioAllocationUseCaseImpl implements GetPortfolioAllocationUseCase {

    private static final int MONEY_SCALE = 2;

    private final GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase;

    public GetPortfolioAllocationUseCaseImpl(GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase) {
        this.getPortfolioHoldingsUseCase = getPortfolioHoldingsUseCase;
    }

    @Override
    public List<AllocationSliceDTO> execute(String email) {
        List<InvestmentLotDTO> lots = getPortfolioHoldingsUseCase.execute(email);

        Map<InvestmentType, BigDecimal> valueByType = lots.stream()
                .collect(Collectors.groupingBy(InvestmentLotDTO::type,
                        Collectors.reducing(BigDecimal.ZERO, InvestmentLotDTO::currentValue, BigDecimal::add)));

        BigDecimal totalCurrentValue = valueByType.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return valueByType.entrySet().stream()
                .map(entry -> {
                    BigDecimal groupValue = entry.getValue();
                    BigDecimal portfolioPercent = totalCurrentValue.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                            : groupValue.multiply(BigDecimal.valueOf(100)).divide(totalCurrentValue, MONEY_SCALE, RoundingMode.HALF_UP);
                    return new AllocationSliceDTO(entry.getKey(), groupValue, portfolioPercent);
                })
                .collect(Collectors.toList());
    }
}
