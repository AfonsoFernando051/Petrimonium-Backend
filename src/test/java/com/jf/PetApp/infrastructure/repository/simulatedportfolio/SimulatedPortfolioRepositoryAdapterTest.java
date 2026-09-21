package com.jf.PetApp.infrastructure.repository.simulatedportfolio;

import com.jf.PetApp.application.simulatedportfolio.port.SimulatedPortfolioRepositoryPort;
import com.jf.PetApp.core.domain.SimulatedOrder;
import com.jf.PetApp.core.domain.SimulatedPortfolio;
import com.jf.PetApp.core.domain.SimulatedPosition;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.SimulatedOrderSide;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;
import com.jf.PetApp.infrastructure.repository.SimulatedOrderRepository;
import com.jf.PetApp.infrastructure.repository.SimulatedPortfolioRepository;
import com.jf.PetApp.infrastructure.repository.SimulatedPositionRepository;
import com.jf.PetApp.infrastructure.repository.user.SpringUserJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SimulatedPortfolioRepositoryAdapterTest {

    @Autowired
    private SimulatedPortfolioRepository portfolioRepository;

    @Autowired
    private SimulatedPositionRepository positionRepository;

    @Autowired
    private SimulatedOrderRepository orderRepository;

    @Autowired
    private SpringUserJpaRepository userJpaRepository;

    private SimulatedPortfolioRepositoryPort adapter;

    private static final String EMAIL = "learner@test.com";

    @BeforeEach
    void setUp() {
        adapter = new SimulatedPortfolioRepositoryAdapter(
                portfolioRepository, positionRepository, orderRepository, userJpaRepository);

        User user = new User();
        user.setUsername("learner");
        user.setEmail(EMAIL);
        user.setPassword("hash");
        userJpaRepository.save(UserJpaEntity.fromDomain(user));
    }

    @Test
    void create_ThenFindByUserEmail_RoundTrips() {
        SimulatedPortfolio created = adapter.create(EMAIL, "BRL");

        assertThat(created.id()).isNotNull();
        assertThat(created.userEmail()).isEqualTo(EMAIL);
        assertThat(created.currency()).isEqualTo("BRL");
        assertThat(created.resetAt()).isNull();

        Optional<SimulatedPortfolio> found = adapter.findByUserEmail(EMAIL);
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(created.id());
    }

    @Test
    void findByUserEmail_WithNoPortfolio_ReturnsEmpty() {
        assertThat(adapter.findByUserEmail(EMAIL)).isEmpty();
    }

    @Test
    void create_ForUnknownEmail_ThrowsIllegalArgumentException() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> adapter.create("ghost@test.com", "BRL"));
    }

    @Test
    void upsertPosition_ThenFindPosition_RoundTrips() {
        SimulatedPortfolio portfolio = adapter.create(EMAIL, "BRL");

        adapter.upsertPosition(portfolio.id(), "PETR4", new BigDecimal("10.000000"), new BigDecimal("30.50"));

        Optional<SimulatedPosition> position = adapter.findPosition(portfolio.id(), "PETR4");
        assertThat(position).isPresent();
        assertThat(position.get().quantity()).isEqualByComparingTo("10.000000");
        assertThat(position.get().averagePrice()).isEqualByComparingTo("30.50");
    }

    @Test
    void upsertPosition_CalledTwice_UpdatesTheSameRowInsteadOfDuplicating() {
        SimulatedPortfolio portfolio = adapter.create(EMAIL, "BRL");

        adapter.upsertPosition(portfolio.id(), "PETR4", new BigDecimal("10.000000"), new BigDecimal("30.50"));
        adapter.upsertPosition(portfolio.id(), "PETR4", new BigDecimal("15.000000"), new BigDecimal("31.00"));

        List<SimulatedPosition> positions = adapter.findPositions(portfolio.id());
        assertThat(positions).hasSize(1);
        assertThat(positions.get(0).quantity()).isEqualByComparingTo("15.000000");
    }

    @Test
    void deletePosition_RemovesIt() {
        SimulatedPortfolio portfolio = adapter.create(EMAIL, "BRL");
        adapter.upsertPosition(portfolio.id(), "PETR4", new BigDecimal("10.000000"), new BigDecimal("30.50"));

        adapter.deletePosition(portfolio.id(), "PETR4");

        assertThat(adapter.findPositions(portfolio.id())).isEmpty();
    }

    @Test
    void saveOrder_ThenFindOrders_RoundTrips() {
        SimulatedPortfolio portfolio = adapter.create(EMAIL, "BRL");

        adapter.saveOrder(portfolio.id(), "PETR4", SimulatedOrderSide.BUY,
                new BigDecimal("10.000000"), new BigDecimal("30.50"), Instant.now(), "order-1");

        List<SimulatedOrder> orders = adapter.findOrders(portfolio.id());
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).clientOrderId()).isEqualTo("order-1");
        assertThat(orders.get(0).side()).isEqualTo(SimulatedOrderSide.BUY);
    }

    @Test
    void saveOrder_PersistsTheGivenExecutionInstantRatherThanNow() {
        SimulatedPortfolio portfolio = adapter.create(EMAIL, "BRL");
        Instant sixMonthsAgo = Instant.parse("2025-03-14T12:00:00Z");

        adapter.saveOrder(portfolio.id(), "PETR4", SimulatedOrderSide.BUY,
                new BigDecimal("10.000000"), new BigDecimal("36.10"), sixMonthsAgo, "order-1");

        assertThat(adapter.findOrders(portfolio.id()).get(0).executedAt()).isEqualTo(sixMonthsAgo);
    }

    @Test
    void findOrderByClientOrderId_FindsTheMatchingOrder() {
        SimulatedPortfolio portfolio = adapter.create(EMAIL, "BRL");
        adapter.saveOrder(portfolio.id(), "PETR4", SimulatedOrderSide.BUY,
                new BigDecimal("10.000000"), new BigDecimal("30.50"), Instant.now(), "order-1");

        Optional<SimulatedOrder> found = adapter.findOrderByClientOrderId(portfolio.id(), "order-1");

        assertThat(found).isPresent();
        assertThat(found.get().ticker()).isEqualTo("PETR4");
    }

    @Test
    void findOrderByClientOrderId_WhenAbsent_ReturnsEmpty() {
        SimulatedPortfolio portfolio = adapter.create(EMAIL, "BRL");

        assertThat(adapter.findOrderByClientOrderId(portfolio.id(), "does-not-exist")).isEmpty();
    }

    @Test
    void resetPortfolio_WipesPositionsAndOrdersAndStampsResetAt() {
        SimulatedPortfolio portfolio = adapter.create(EMAIL, "BRL");
        adapter.upsertPosition(portfolio.id(), "PETR4", new BigDecimal("10.000000"), new BigDecimal("30.50"));
        adapter.saveOrder(portfolio.id(), "PETR4", SimulatedOrderSide.BUY,
                new BigDecimal("10.000000"), new BigDecimal("30.50"), Instant.now(), "order-1");

        adapter.resetPortfolio(portfolio.id());

        SimulatedPortfolio reloaded = adapter.findByUserEmail(EMAIL).orElseThrow();
        assertThat(reloaded.resetAt()).isNotNull();
        assertThat(adapter.findPositions(portfolio.id())).isEmpty();
        assertThat(adapter.findOrders(portfolio.id())).isEmpty();
    }

    @Test
    void findByUserEmail_IsolatedPerUser() {
        User otherUser = new User();
        otherUser.setUsername("other-learner");
        otherUser.setEmail("other-learner@test.com");
        otherUser.setPassword("hash");
        userJpaRepository.save(UserJpaEntity.fromDomain(otherUser));

        SimulatedPortfolio own = adapter.create(EMAIL, "BRL");
        SimulatedPortfolio other = adapter.create("other-learner@test.com", "BRL");

        assertThat(adapter.findByUserEmail(EMAIL).orElseThrow().id()).isEqualTo(own.id());
        assertThat(adapter.findByUserEmail("other-learner@test.com").orElseThrow().id()).isEqualTo(other.id());
        assertThat(own.id()).isNotEqualTo(other.id());
    }
}
