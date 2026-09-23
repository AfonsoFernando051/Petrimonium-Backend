package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;
import com.jf.PetApp.application.simulatedportfolio.SimulatedOrderMessages;
import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedOrderDTO;
import com.jf.PetApp.application.simulatedportfolio.port.SimulatedPortfolioRepositoryPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.SimulatedOrder;
import com.jf.PetApp.core.domain.SimulatedPortfolio;
import com.jf.PetApp.core.domain.SimulatedPosition;
import com.jf.PetApp.core.domain.enums.SimulatedOrderSide;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Executes one simulated buy or sell at the current reference market price
 * (never a client-supplied price — same reasoning as never trusting a
 * client-supplied user id, applied to money: a simulated fill still has to
 * be realistic to be educational, and a client-chosen price would make the
 * "practice on real reference prices" premise meaningless).
 *
 * <p>A past {@code tradeDate} backdates the order: it fills at that day's
 * historical close (the most recent trading day on or before it) and is
 * stamped at noon UTC of that date — noon, not midnight, so the calendar
 * day survives a conversion to any local timezone. That is what lets a
 * student assemble a portfolio "as if" they had bought months ago. The
 * wallet has no cash or balance — it only holds the positions the student
 * registers — so no order is ever refused for lack of funds. A placeholder
 * quote (the dev-only fabricated price served when no market-data token is
 * configured) is refused like a missing one, so no invented price is ever
 * stored as a fill. Uses the same
 * {@link ExternalInvestmentApiPort} the real_portfolio context uses for
 * quotes — a deliberate, narrow exception to the simulated/real boundary
 * (read-only public market data, not portfolio state) — see
 * {@code SimulatedPortfolioBoundaryTest}.
 */
@Service
public class PlaceSimulatedOrderUseCaseImpl implements PlaceSimulatedOrderUseCase {

    private static final LocalTime BACKDATED_EXECUTION_TIME = LocalTime.NOON;

    private final GetOrCreateSimulatedPortfolioUseCase getOrCreateSimulatedPortfolioUseCase;
    private final SimulatedPortfolioRepositoryPort simulatedPortfolioRepository;
    private final ExternalInvestmentApiPort externalInvestmentApiPort;

    /**
     * Only ever read for {@code preferredLanguage}: every rejection below is shown to the learner
     * verbatim by the client, so it has to be written in their language — see
     * {@link SimulatedOrderMessages}. Same reason the Mentor's use case holds this port.
     */
    private final UserRepository userRepository;

    public PlaceSimulatedOrderUseCaseImpl(
            GetOrCreateSimulatedPortfolioUseCase getOrCreateSimulatedPortfolioUseCase,
            SimulatedPortfolioRepositoryPort simulatedPortfolioRepository,
            ExternalInvestmentApiPort externalInvestmentApiPort,
            UserRepository userRepository
    ) {
        this.getOrCreateSimulatedPortfolioUseCase = getOrCreateSimulatedPortfolioUseCase;
        this.simulatedPortfolioRepository = simulatedPortfolioRepository;
        this.externalInvestmentApiPort = externalInvestmentApiPort;
        this.userRepository = userRepository;
    }

    /** Falls back to the default language rather than failing the request over missing copy. */
    private String languageOf(String email) {
        return userRepository.findByEmail(email).map(User::getPreferredLanguage).orElse(null);
    }

    @Override
    @Transactional
    public SimulatedOrderDTO execute(String email, PlaceSimulatedOrderCommand command) {
        String language = languageOf(email);
        if (command.quantity() == null || command.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(SimulatedOrderMessages.quantityMustBePositive(language));
        }
        if (command.ticker() == null || command.ticker().isBlank()) {
            throw new IllegalArgumentException(SimulatedOrderMessages.tickerMustNotBeBlank(language));
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

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate tradeDate = command.tradeDate();
        if (tradeDate != null && tradeDate.isAfter(today)) {
            throw new IllegalArgumentException(SimulatedOrderMessages.tradeDateMustNotBeInTheFuture(language));
        }
        boolean backdated = tradeDate != null && tradeDate.isBefore(today);

        BigDecimal price = backdated
                ? resolveHistoricalPrice(ticker, tradeDate, language)
                : resolveReferencePrice(ticker, language);
        Instant executedAt = backdated ? tradeDate.atTime(BACKDATED_EXECUTION_TIME).toInstant(ZoneOffset.UTC) : Instant.now();
        BigDecimal quantity = command.quantity();

        if (backdated && command.side() == SimulatedOrderSide.SELL) {
            requireHeldSince(portfolio, ticker, tradeDate, language);
        }

        SimulatedOrder order = switch (command.side()) {
            case BUY -> executeBuy(portfolio, ticker, quantity, price, executedAt, clientOrderId);
            case SELL -> executeSell(portfolio, ticker, quantity, price, executedAt, clientOrderId, language);
        };

        return toDto(order);
    }

    private BigDecimal resolveReferencePrice(String ticker, String language) {
        AssetQuoteResponse quote = externalInvestmentApiPort.getQuote(ticker)
                .orElseThrow(() -> new IllegalArgumentException(
                        SimulatedOrderMessages.unknownTicker(language, ticker)));
        if (quote.regularMarketPrice() == null || quote.simulated()) {
            throw new IllegalArgumentException(SimulatedOrderMessages.noReferencePrice(language, ticker));
        }
        return BigDecimal.valueOf(quote.regularMarketPrice()).setScale(2, RoundingMode.HALF_UP);
    }

    /** Empty means no close on or before {@code date} — never falls back to today's price. */
    private BigDecimal resolveHistoricalPrice(String ticker, LocalDate date, String language) {
        AssetQuoteResponse quote = externalInvestmentApiPort.getQuoteAtDate(ticker, date)
                .orElseThrow(() -> new IllegalArgumentException(
                        SimulatedOrderMessages.noHistoricalPrice(language, ticker, date)));
        if (quote.regularMarketPrice() == null || quote.simulated()) {
            throw new IllegalArgumentException(SimulatedOrderMessages.noHistoricalPrice(language, ticker, date));
        }
        return BigDecimal.valueOf(quote.regularMarketPrice()).setScale(2, RoundingMode.HALF_UP);
    }

    // A sell dated before the first buy of that ticker would leave the order ledger claiming the
    // student sold something they did not yet own, which the wealth-evolution chart replays.
    private void requireHeldSince(SimulatedPortfolio portfolio, String ticker, LocalDate sellDate, String language) {
        Optional<LocalDate> firstBuyDate = simulatedPortfolioRepository.findOrders(portfolio.id()).stream()
                .filter(o -> o.side() == SimulatedOrderSide.BUY && o.ticker().equals(ticker))
                .map(o -> o.executedAt().atZone(ZoneOffset.UTC).toLocalDate())
                .min(LocalDate::compareTo);
        if (firstBuyDate.isPresent() && sellDate.isBefore(firstBuyDate.get())) {
            throw new IllegalArgumentException(
                    SimulatedOrderMessages.sellBeforeFirstBuy(language, ticker, sellDate, firstBuyDate.get()));
        }
    }

    private SimulatedOrder executeBuy(
            SimulatedPortfolio portfolio, String ticker, BigDecimal quantity, BigDecimal price,
            Instant executedAt, String clientOrderId
    ) {
        Optional<SimulatedPosition> existingPosition =
                simulatedPortfolioRepository.findPosition(portfolio.id(), ticker);
        BigDecimal newQuantity = existingPosition.map(SimulatedPosition::quantity).orElse(BigDecimal.ZERO).add(quantity);
        BigDecimal newAveragePrice = existingPosition
                .map(p -> weightedAveragePrice(p.quantity(), p.averagePrice(), quantity, price))
                .orElse(price);

        simulatedPortfolioRepository.upsertPosition(portfolio.id(), ticker, newQuantity, newAveragePrice);

        return simulatedPortfolioRepository.saveOrder(
                portfolio.id(), ticker, SimulatedOrderSide.BUY, quantity, price, executedAt, clientOrderId);
    }

    private SimulatedOrder executeSell(
            SimulatedPortfolio portfolio, String ticker, BigDecimal quantity, BigDecimal price,
            Instant executedAt, String clientOrderId, String language
    ) {
        SimulatedPosition position = simulatedPortfolioRepository.findPosition(portfolio.id(), ticker)
                .orElseThrow(() -> new IllegalArgumentException(
                        SimulatedOrderMessages.noPositionHeld(language, ticker)));

        if (quantity.compareTo(position.quantity()) > 0) {
            throw new IllegalArgumentException(
                    SimulatedOrderMessages.insufficientQuantity(language, ticker, position.quantity(), quantity));
        }

        BigDecimal remainingQuantity = position.quantity().subtract(quantity);

        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            simulatedPortfolioRepository.deletePosition(portfolio.id(), ticker);
        } else {
            simulatedPortfolioRepository.upsertPosition(portfolio.id(), ticker, remainingQuantity, position.averagePrice());
        }

        return simulatedPortfolioRepository.saveOrder(
                portfolio.id(), ticker, SimulatedOrderSide.SELL, quantity, price, executedAt, clientOrderId);
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
