package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;
import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedOrderDTO;
import com.jf.PetApp.application.simulatedportfolio.port.SimulatedPortfolioRepositoryPort;
import com.jf.PetApp.core.domain.SimulatedOrder;
import com.jf.PetApp.core.domain.SimulatedPortfolio;
import com.jf.PetApp.core.domain.SimulatedPosition;
import com.jf.PetApp.core.domain.enums.SimulatedOrderSide;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

/**
 * Executes one simulated buy or sell at the current reference market price
 * (never a client-supplied price — same reasoning as never trusting a
 * client-supplied user id, applied to money: a simulated fill still has to
 * be realistic to be educational, and a client-chosen price would make the
 * "practice on real reference prices" premise meaningless). Uses the same
 * {@link ExternalInvestmentApiPort} the real_portfolio context uses for
 * quotes — a deliberate, narrow exception to the simulated/real boundary
 * (read-only public market data, not portfolio state) — see
 * {@code SimulatedPortfolioBoundaryTest}.
 */
@Service
public class PlaceSimulatedOrderUseCaseImpl implements PlaceSimulatedOrderUseCase {

    private final GetOrCreateSimulatedPortfolioUseCase getOrCreateSimulatedPortfolioUseCase;
    private final SimulatedPortfolioRepositoryPort simulatedPortfolioRepository;
    private final ExternalInvestmentApiPort externalInvestmentApiPort;

    public PlaceSimulatedOrderUseCaseImpl(
            GetOrCreateSimulatedPortfolioUseCase getOrCreateSimulatedPortfolioUseCase,
            SimulatedPortfolioRepositoryPort simulatedPortfolioRepository,
            ExternalInvestmentApiPort externalInvestmentApiPort
    ) {
        this.getOrCreateSimulatedPortfolioUseCase = getOrCreateSimulatedPortfolioUseCase;
        this.simulatedPortfolioRepository = simulatedPortfolioRepository;
        this.externalInvestmentApiPort = externalInvestmentApiPort;
    }

    @Override
    @Transactional
    public SimulatedOrderDTO execute(String email, PlaceSimulatedOrderCommand command) {
        if (command.quantity() == null || command.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order quantity must be greater than zero");
        }
        if (command.ticker() == null || command.ticker().isBlank()) {
            throw new IllegalArgumentException("Order ticker must not be blank");
        }

        SimulatedPortfolio portfolio = getOrCreateSimulatedPortfolioUseCase.execute(email);
        String ticker = command.ticker().trim().toUpperCase();
        String clientOrderId = command.clientOrderId() != null && !command.clientOrderId().isBlank()
                ? command.clientOrderId().trim()
                : UUID.randomUUID().toString();

        Optional<SimulatedOrder> existing =
                simulatedPortfolioRepository.findOrderByClientOrderId(portfolio.id(), clientOrderId);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        BigDecimal price = resolveReferencePrice(ticker);
        BigDecimal quantity = command.quantity();

        SimulatedOrder order = switch (command.side()) {
            case BUY -> executeBuy(portfolio, ticker, quantity, price, clientOrderId);
            case SELL -> executeSell(portfolio, ticker, quantity, price, clientOrderId);
        };

        return toDto(order);
    }

    private BigDecimal resolveReferencePrice(String ticker) {
        AssetQuoteResponse quote = externalInvestmentApiPort.getQuote(ticker)
                .orElseThrow(() -> new IllegalArgumentException("Unknown ticker for simulation: " + ticker));
        if (quote.regularMarketPrice() == null) {
            throw new IllegalArgumentException("No reference price available for ticker: " + ticker);
        }
        return BigDecimal.valueOf(quote.regularMarketPrice()).setScale(2, RoundingMode.HALF_UP);
    }

    private SimulatedOrder executeBuy(
            SimulatedPortfolio portfolio, String ticker, BigDecimal quantity, BigDecimal price, String clientOrderId
    ) {
        BigDecimal cost = price.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        if (cost.compareTo(portfolio.virtualBalance()) > 0) {
            throw new IllegalArgumentException("Insufficient virtual balance to buy " + quantity + " " + ticker);
        }

        Optional<SimulatedPosition> existingPosition =
                simulatedPortfolioRepository.findPosition(portfolio.id(), ticker);
        BigDecimal newQuantity = existingPosition.map(SimulatedPosition::quantity).orElse(BigDecimal.ZERO).add(quantity);
        BigDecimal newAveragePrice = existingPosition
                .map(p -> weightedAveragePrice(p.quantity(), p.averagePrice(), quantity, price))
                .orElse(price);

        simulatedPortfolioRepository.upsertPosition(portfolio.id(), ticker, newQuantity, newAveragePrice);
        simulatedPortfolioRepository.updateBalance(portfolio.id(), portfolio.virtualBalance().subtract(cost));

        return simulatedPortfolioRepository.saveOrder(
                portfolio.id(), ticker, SimulatedOrderSide.BUY, quantity, price, clientOrderId);
    }

    private SimulatedOrder executeSell(
            SimulatedPortfolio portfolio, String ticker, BigDecimal quantity, BigDecimal price, String clientOrderId
    ) {
        SimulatedPosition position = simulatedPortfolioRepository.findPosition(portfolio.id(), ticker)
                .orElseThrow(() -> new IllegalArgumentException("No simulated position held in " + ticker));

        if (quantity.compareTo(position.quantity()) > 0) {
            throw new IllegalArgumentException(
                    "Insufficient simulated position quantity in " + ticker + " to sell " + quantity);
        }

        BigDecimal proceeds = price.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingQuantity = position.quantity().subtract(quantity);

        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            simulatedPortfolioRepository.deletePosition(portfolio.id(), ticker);
        } else {
            simulatedPortfolioRepository.upsertPosition(portfolio.id(), ticker, remainingQuantity, position.averagePrice());
        }
        simulatedPortfolioRepository.updateBalance(portfolio.id(), portfolio.virtualBalance().add(proceeds));

        return simulatedPortfolioRepository.saveOrder(
                portfolio.id(), ticker, SimulatedOrderSide.SELL, quantity, price, clientOrderId);
    }

    // Cost-weighted average, same formula used to accumulate a real position's average price
    // (see UserPositionCalculator) — rounded to the same 2-decimal money scale everything else
    // in this context uses.
    private BigDecimal weightedAveragePrice(
            BigDecimal existingQuantity, BigDecimal existingAveragePrice, BigDecimal addedQuantity, BigDecimal addedPrice
    ) {
        BigDecimal totalCost = existingQuantity.multiply(existingAveragePrice).add(addedQuantity.multiply(addedPrice));
        BigDecimal totalQuantity = existingQuantity.add(addedQuantity);
        return totalCost.divide(totalQuantity, 2, RoundingMode.HALF_UP);
    }

    private SimulatedOrderDTO toDto(SimulatedOrder order) {
        return new SimulatedOrderDTO(
                order.id(),
                order.ticker(),
                order.side(),
                order.quantity(),
                order.price(),
                order.price().multiply(order.quantity()).setScale(2, RoundingMode.HALF_UP),
                order.executedAt(),
                order.clientOrderId()
        );
    }
}
