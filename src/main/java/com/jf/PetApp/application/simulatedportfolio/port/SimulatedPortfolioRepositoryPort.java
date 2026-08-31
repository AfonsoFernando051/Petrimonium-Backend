package com.jf.PetApp.application.simulatedportfolio.port;

import com.jf.PetApp.core.domain.SimulatedOrder;
import com.jf.PetApp.core.domain.SimulatedPortfolio;
import com.jf.PetApp.core.domain.SimulatedPosition;
import com.jf.PetApp.core.domain.enums.SimulatedOrderSide;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Application-layer boundary for simulated-portfolio persistence. Use cases
 * depend on this port, never on Spring Data or JPA entities directly — the
 * adapter in {@code infrastructure.repository.simulatedportfolio} is the
 * only place that knows how the simulated wallet is actually stored.
 *
 * <p>Deliberately never touches {@code InvestmentRepositoryPort} or any
 * real_portfolio entity — see docs/BACKEND_MODULE_PLAN.md §2 and the
 * ArchUnit rule in {@code SimulatedPortfolioBoundaryTest}.
 */
public interface SimulatedPortfolioRepositoryPort {

    Optional<SimulatedPortfolio> findByUserEmail(String userEmail);

    SimulatedPortfolio create(String userEmail, BigDecimal initialBalance, String currency);

    void updateBalance(Long portfolioId, BigDecimal newBalance);

    List<SimulatedPosition> findPositions(Long portfolioId);

    Optional<SimulatedPosition> findPosition(Long portfolioId, String ticker);

    void upsertPosition(Long portfolioId, String ticker, BigDecimal quantity, BigDecimal averagePrice);

    void deletePosition(Long portfolioId, String ticker);

    List<SimulatedOrder> findOrders(Long portfolioId);

    Optional<SimulatedOrder> findOrderByClientOrderId(Long portfolioId, String clientOrderId);

    SimulatedOrder saveOrder(
            Long portfolioId,
            String ticker,
            SimulatedOrderSide side,
            BigDecimal quantity,
            BigDecimal price,
            String clientOrderId
    );

    /**
     * Wipes every position and order for {@code portfolioId} and resets its
     * balance to {@code initialBalance} — "reiniciar a simulação". Requires
     * explicit user confirmation upstream (see
     * ResetSimulatedPortfolioUseCaseImpl); this method itself performs the
     * reset unconditionally once called.
     */
    void resetPortfolio(Long portfolioId, BigDecimal initialBalance);
}
