package com.jf.PetApp.application.investment.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.jf.PetApp.core.domain.enums.PriceStatus;

@Service
public class GetPortfolioHoldingsUseCaseImpl implements GetPortfolioHoldingsUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetPortfolioHoldingsUseCaseImpl.class);

    private static final int MONEY_SCALE = 2;

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

        Map<String, PricedQuote> priceCache = new HashMap<>();
        for (Investment lot : lots) {
            priceCache.computeIfAbsent(lot.name(), ticker -> fetchCurrentPrice(ticker, lot.type(), lot.purchasePrice()));
        }

        return lots.stream().map(lot -> {
            PricedQuote quote = priceCache.get(lot.name());
            BigDecimal currentPrice = quote.price();
            BigDecimal investedValue = money(lot.quantity().multiply(lot.purchasePrice()));
            BigDecimal currentValue = money(lot.quantity().multiply(currentPrice));
            return new InvestmentLotDTO(
                    lot.id(),
                    lot.name(),
                    lot.type(),
                    lot.quantity(),
                    lot.purchasePrice(),
                    lot.purchaseDate(),
                    currentPrice,
                    investedValue,
                    currentValue,
                    quote.status()
            );
        }).collect(Collectors.toList());
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * A price plus the provenance of that price. Kept together so a caller can never read the
     * number without also being handed the answer to "is this real?" — the two used to be
     * separable, and every fallback silently became a 0% return on screen.
     */
    private record PricedQuote(BigDecimal price, PriceStatus status) {
    }

    private PricedQuote fetchCurrentPrice(String ticker, InvestmentType type, BigDecimal fallbackPrice) {
        // Tesouro Direto / fixed-income bonds aren't equities and have no ticker on
        // Brapi's quote feed — querying it always 404s. No accrual-based pricing
        // model exists yet, so fall back to the purchase price directly. NOT_QUOTED
        // rather than STALE: retrying will never produce a quote for this asset class.
        if (type == InvestmentType.FIXED_INCOME) {
            return new PricedQuote(fallbackPrice, PriceStatus.NOT_QUOTED);
        }
        try {
            return externalInvestmentApiPort.getQuote(ticker)
                    // A placeholder quote is not a price. Dropping it here means it takes the
                    // same STALE_PURCHASE_PRICE path as a provider outage, so a fabricated
                    // number can never reach a real portfolio valuation labelled as LIVE.
                    .filter(quote -> !quote.simulated())
                    .map(AssetQuoteResponse::regularMarketPrice)
                    .filter(price -> price != null)
                    // The quote feed is an external market-data source and stays
                    // Double; this is the single boundary where it enters the
                    // BigDecimal ledger chain.
                    .map(price -> new PricedQuote(money(BigDecimal.valueOf(price)), PriceStatus.LIVE))
                    .orElseGet(() -> {
                        log.warn("No quote available for ticker {}; reporting purchase price as stale", ticker);
                        return new PricedQuote(fallbackPrice, PriceStatus.STALE_PURCHASE_PRICE);
                    });
        } catch (Exception e) {
            log.warn("Failed to fetch quote for ticker {}, falling back to purchase price: {}", ticker, e.getMessage());
            return new PricedQuote(fallbackPrice, PriceStatus.STALE_PURCHASE_PRICE);
        }
    }
}
