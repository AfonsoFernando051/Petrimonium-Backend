package com.jf.PetApp.application.investment.usecase;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.jf.PetApp.application.investment.dto.AllocationSliceDTO;
import com.jf.PetApp.application.investment.dto.InvestmentLotDTO;
import com.jf.PetApp.core.domain.enums.InvestmentType;

@Service
public class GetPortfolioAllocationUseCaseImpl implements GetPortfolioAllocationUseCase {

    private final GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase;

    public GetPortfolioAllocationUseCaseImpl(GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase) {
        this.getPortfolioHoldingsUseCase = getPortfolioHoldingsUseCase;
    }

    @Override
    public List<AllocationSliceDTO> execute(String email) {
        List<InvestmentLotDTO> lots = getPortfolioHoldingsUseCase.execute(email);

        Map<InvestmentType, Double> valueByType = lots.stream()
                .collect(Collectors.groupingBy(InvestmentLotDTO::type,
                        Collectors.summingDouble(InvestmentLotDTO::currentValue)));

        double totalCurrentValue = valueByType.values().stream().mapToDouble(Double::doubleValue).sum();

        return valueByType.entrySet().stream()
                .map(entry -> {
                    Double groupValue = entry.getValue();
                    Double portfolioPercent = totalCurrentValue == 0.0 ? 0.0 : (groupValue / totalCurrentValue) * 100;
                    return new AllocationSliceDTO(entry.getKey(), groupValue, portfolioPercent);
                })
                .collect(Collectors.toList());
    }
}
