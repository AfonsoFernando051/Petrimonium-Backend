package com.jf.PetApp.application.investment.usecase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.dto.InvestmentLotDTO;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;
import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import com.jf.PetApp.core.domain.Investment;
import com.jf.PetApp.core.domain.enums.InvestmentType;

@Service
public class GetPortfolioHoldingsUseCaseImpl implements GetPortfolioHoldingsUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetPortfolioHoldingsUseCaseImpl.class);

    private final InvestmentRepositoryPort investmentRepositoryPort;
    private final ExternalInvestmentApiPort externalInvestmentApiPort;

    public GetPortfolioHoldingsUseCaseImpl(InvestmentRepositoryPort investmentRepositoryPort,
                                            ExternalInvestmentApiPort externalInvestmentApiPort) {
        this.investmentRepositoryPort = investmentRepositoryPort;
        this.externalInvestmentApiPort = externalInvestmentApiPort;
    }

    @Override
    public List<InvestmentLotDTO> execute(String email) {
        List<Investment> lots = investmentRepositoryPort.findByUserEmail(email);

        Map<String, Double> priceCache = new HashMap<>();
        for (Investment lot : lots) {
            priceCache.computeIfAbsent(lot.name(), ticker -> fetchCurrentPrice(ticker, lot.type(), lot.purchasePrice()));
        }

        return lots.stream().map(lot -> {
            Double currentPrice = priceCache.get(lot.name());
            Double investedValue = lot.quantity() * lot.purchasePrice();
            Double currentValue = lot.quantity() * currentPrice;
            return new InvestmentLotDTO(
                    lot.id(),
                    lot.name(),
                    lot.type(),
                    lot.quantity(),
                    lot.purchasePrice(),
                    lot.purchaseDate(),
                    currentPrice,
                    investedValue,
                    currentValue
            );
        }).collect(Collectors.toList());
    }

    private Double fetchCurrentPrice(String ticker, InvestmentType type, Double fallbackPrice) {
        // Tesouro Direto / fixed-income bonds aren't equities and have no ticker on
        // Brapi's quote feed — querying it always 404s. No accrual-based pricing
        // model exists yet, so fall back to the purchase price directly.
        if (type == InvestmentType.FIXED_INCOME) {
            return fallbackPrice;
        }
        try {
            return externalInvestmentApiPort.getQuote(ticker)
                    .map(AssetQuoteResponse::regularMarketPrice)
                    .filter(price -> price != null)
                    .orElse(fallbackPrice);
        } catch (Exception e) {
            log.warn("Failed to fetch quote for ticker {}, falling back to purchase price: {}", ticker, e.getMessage());
            return fallbackPrice;
        }
    }
}
