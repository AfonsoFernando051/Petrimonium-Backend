package com.jf.PetApp.infrastructure.repository.simulatedportfolio;

import com.jf.PetApp.application.simulatedportfolio.port.SimulatedPortfolioRepositoryPort;
import com.jf.PetApp.core.domain.SimulatedOrder;
import com.jf.PetApp.core.domain.SimulatedPortfolio;
import com.jf.PetApp.core.domain.SimulatedPosition;
import com.jf.PetApp.core.domain.enums.SimulatedOrderSide;
import com.jf.PetApp.infrastructure.entity.SimulatedOrderJpaEntity;
import com.jf.PetApp.infrastructure.entity.SimulatedPortfolioJpaEntity;
import com.jf.PetApp.infrastructure.entity.SimulatedPositionJpaEntity;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;
import com.jf.PetApp.infrastructure.repository.SimulatedOrderRepository;
import com.jf.PetApp.infrastructure.repository.SimulatedPortfolioRepository;
import com.jf.PetApp.infrastructure.repository.SimulatedPositionRepository;
import com.jf.PetApp.infrastructure.repository.user.SpringUserJpaRepository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The only place in the codebase that knows the simulated wallet is stored
 * as JPA entities. Implements {@link SimulatedPortfolioRepositoryPort} so
 * every use case upstream works with the plain domain records instead.
 */
@Repository
public class SimulatedPortfolioRepositoryAdapter implements SimulatedPortfolioRepositoryPort {

    private final SimulatedPortfolioRepository portfolioRepository;
    private final SimulatedPositionRepository positionRepository;
    private final SimulatedOrderRepository orderRepository;
    private final SpringUserJpaRepository userJpaRepository;

    public SimulatedPortfolioRepositoryAdapter(
            SimulatedPortfolioRepository portfolioRepository,
            SimulatedPositionRepository positionRepository,
            SimulatedOrderRepository orderRepository,
            SpringUserJpaRepository userJpaRepository
    ) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.orderRepository = orderRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Optional<SimulatedPortfolio> findByUserEmail(String userEmail) {
        return portfolioRepository.findByUser_Email(userEmail).map(entity -> toDomain(entity, userEmail));
    }

    @Override
    @Transactional
    public SimulatedPortfolio create(String userEmail, BigDecimal initialBalance, String currency) {
        UserJpaEntity user = userJpaRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + userEmail));

        Instant now = Instant.now();
        SimulatedPortfolioJpaEntity entity = new SimulatedPortfolioJpaEntity();
        entity.setUser(user);
        entity.setVirtualBalance(initialBalance);
        entity.setInitialBalance(initialBalance);
        entity.setCurrency(currency);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        return toDomain(portfolioRepository.save(entity), userEmail);
    }

    @Override
    @Transactional
    public void updateBalance(Long portfolioId, BigDecimal newBalance) {
        SimulatedPortfolioJpaEntity entity = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Simulated portfolio not found: " + portfolioId));
        entity.setVirtualBalance(newBalance);
        entity.setUpdatedAt(Instant.now());
        portfolioRepository.save(entity);
    }

    @Override
    public List<SimulatedPosition> findPositions(Long portfolioId) {
        return positionRepository.findByPortfolio_IdOrderByTicker(portfolioId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SimulatedPosition> findPosition(Long portfolioId, String ticker) {
        return positionRepository.findByPortfolio_IdAndTicker(portfolioId, ticker).map(this::toDomain);
    }

    @Override
    @Transactional
    public void upsertPosition(Long portfolioId, String ticker, BigDecimal quantity, BigDecimal averagePrice) {
        SimulatedPortfolioJpaEntity portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Simulated portfolio not found: " + portfolioId));

        SimulatedPositionJpaEntity entity = positionRepository.findByPortfolio_IdAndTicker(portfolioId, ticker)
                .orElseGet(SimulatedPositionJpaEntity::new);

        Instant now = Instant.now();
        if (entity.getId() == null) {
            entity.setPortfolio(portfolio);
            entity.setTicker(ticker);
            entity.setCreatedAt(now);
        }
        entity.setQuantity(quantity);
        entity.setAveragePrice(averagePrice);
        entity.setUpdatedAt(now);
        positionRepository.save(entity);
    }

    @Override
    @Transactional
    public void deletePosition(Long portfolioId, String ticker) {
        positionRepository.deleteByPortfolioIdAndTicker(portfolioId, ticker);
    }

    @Override
    public List<SimulatedOrder> findOrders(Long portfolioId) {
        return orderRepository.findByPortfolio_IdOrderByExecutedAtDesc(portfolioId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SimulatedOrder> findOrderByClientOrderId(Long portfolioId, String clientOrderId) {
        return orderRepository.findByPortfolio_IdAndClientOrderId(portfolioId, clientOrderId).map(this::toDomain);
    }

    @Override
    @Transactional
    public SimulatedOrder saveOrder(
            Long portfolioId,
            String ticker,
            SimulatedOrderSide side,
            BigDecimal quantity,
            BigDecimal price,
            String clientOrderId
    ) {
        SimulatedPortfolioJpaEntity portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Simulated portfolio not found: " + portfolioId));

        SimulatedOrderJpaEntity entity = new SimulatedOrderJpaEntity();
        entity.setPortfolio(portfolio);
        entity.setTicker(ticker);
        entity.setSide(side);
        entity.setQuantity(quantity);
        entity.setPrice(price);
        entity.setExecutedAt(Instant.now());
        entity.setClientOrderId(clientOrderId);

        return toDomain(orderRepository.save(entity));
    }

    @Override
    @Transactional
    public void resetPortfolio(Long portfolioId, BigDecimal initialBalance) {
        orderRepository.deleteByPortfolioId(portfolioId);
        positionRepository.deleteByPortfolioId(portfolioId);

        SimulatedPortfolioJpaEntity entity = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Simulated portfolio not found: " + portfolioId));
        Instant now = Instant.now();
        entity.setVirtualBalance(initialBalance);
        entity.setResetAt(now);
        entity.setUpdatedAt(now);
        portfolioRepository.save(entity);
    }

    private SimulatedPortfolio toDomain(SimulatedPortfolioJpaEntity entity, String userEmail) {
        return new SimulatedPortfolio(
                entity.getId(),
                userEmail,
                entity.getVirtualBalance(),
                entity.getInitialBalance(),
                entity.getCurrency(),
                entity.getResetAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private SimulatedPosition toDomain(SimulatedPositionJpaEntity entity) {
        return new SimulatedPosition(
                entity.getId(),
                entity.getPortfolio().getId(),
                entity.getTicker(),
                entity.getQuantity(),
                entity.getAveragePrice()
        );
    }

    private SimulatedOrder toDomain(SimulatedOrderJpaEntity entity) {
        return new SimulatedOrder(
                entity.getId(),
                entity.getPortfolio().getId(),
                entity.getTicker(),
                entity.getSide(),
                entity.getQuantity(),
                entity.getPrice(),
                entity.getExecutedAt(),
                entity.getClientOrderId()
        );
    }
}
