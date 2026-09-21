package com.jf.PetApp.application.simulatedportfolio.usecase;

import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;
import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedOrderDTO;
import com.jf.PetApp.application.simulatedportfolio.port.SimulatedPortfolioRepositoryPort;
import com.jf.PetApp.core.domain.SimulatedOrder;
import com.jf.PetApp.core.domain.SimulatedPortfolio;
import com.jf.PetApp.core.domain.SimulatedPosition;
import com.jf.PetApp.core.domain.enums.SimulatedOrderSide;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceSimulatedOrderUseCaseImplTest {

    @Mock
    private GetOrCreateSimulatedPortfolioUseCase getOrCreateSimulatedPortfolioUseCase;

    @Mock
    private SimulatedPortfolioRepositoryPort simulatedPortfolioRepository;

    @Mock
    private ExternalInvestmentApiPort externalInvestmentApiPort;

    private PlaceSimulatedOrderUseCaseImpl useCase;

    private static final String EMAIL = "learner@test.com";
    private static final Long PORTFOLIO_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new PlaceSimulatedOrderUseCaseImpl(
                getOrCreateSimulatedPortfolioUseCase, simulatedPortfolioRepository, externalInvestmentApiPort);
    }

    private SimulatedPortfolio portfolioWithBalance(BigDecimal balance) {
        return new SimulatedPortfolio(
                PORTFOLIO_ID, EMAIL, balance, new BigDecimal("10000.00"), "BRL", null, Instant.now(), Instant.now());
    }

    @Test
    void execute_Buy_WithSufficientBalance_DebitsBalanceAndCreatesPosition() {
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL))
                .thenReturn(portfolioWithBalance(new BigDecimal("10000.00")));
        when(externalInvestmentApiPort.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 30.50, "BRL")));
        when(simulatedPortfolioRepository.findOrderByClientOrderId(eq(PORTFOLIO_ID), anyString()))
                .thenReturn(Optional.empty());
        when(simulatedPortfolioRepository.findPosition(PORTFOLIO_ID, "PETR4")).thenReturn(Optional.empty());
        when(simulatedPortfolioRepository.saveOrder(eq(PORTFOLIO_ID), eq("PETR4"), eq(SimulatedOrderSide.BUY),
                any(), any(), any(), anyString()))
                .thenReturn(new SimulatedOrder(1L, PORTFOLIO_ID, "PETR4", SimulatedOrderSide.BUY,
                        new BigDecimal("10"), new BigDecimal("30.50"), Instant.now(), "generated-id"));

        SimulatedOrderDTO result = useCase.execute(EMAIL,
                new PlaceSimulatedOrderCommand("petr4", SimulatedOrderSide.BUY, new BigDecimal("10"), null));

        assertEquals("PETR4", result.ticker());
        verify(simulatedPortfolioRepository).upsertPosition(
                eq(PORTFOLIO_ID), eq("PETR4"), eq(new BigDecimal("10")), eq(new BigDecimal("30.50")));
        verify(simulatedPortfolioRepository).updateBalance(PORTFOLIO_ID, new BigDecimal("9695.00"));
    }

    @Test
    void execute_Buy_WithInsufficientBalance_ThrowsAndNeverTouchesPositionOrBalance() {
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL))
                .thenReturn(portfolioWithBalance(new BigDecimal("100.00")));
        when(externalInvestmentApiPort.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 30.50, "BRL")));
        when(simulatedPortfolioRepository.findOrderByClientOrderId(eq(PORTFOLIO_ID), anyString()))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL,
                new PlaceSimulatedOrderCommand("PETR4", SimulatedOrderSide.BUY, new BigDecimal("10"), null)));

        verify(simulatedPortfolioRepository, never()).upsertPosition(any(), any(), any(), any());
        verify(simulatedPortfolioRepository, never()).updateBalance(any(), any());
        verify(simulatedPortfolioRepository, never()).saveOrder(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void execute_Buy_AddsToAnExistingPositionWithWeightedAveragePrice() {
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL))
                .thenReturn(portfolioWithBalance(new BigDecimal("10000.00")));
        when(externalInvestmentApiPort.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 40.00, "BRL")));
        when(simulatedPortfolioRepository.findOrderByClientOrderId(eq(PORTFOLIO_ID), anyString()))
                .thenReturn(Optional.empty());
        when(simulatedPortfolioRepository.findPosition(PORTFOLIO_ID, "PETR4"))
                .thenReturn(Optional.of(new SimulatedPosition(1L, PORTFOLIO_ID, "PETR4",
                        new BigDecimal("10"), new BigDecimal("30.00"))));
        when(simulatedPortfolioRepository.saveOrder(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new SimulatedOrder(2L, PORTFOLIO_ID, "PETR4", SimulatedOrderSide.BUY,
                        new BigDecimal("10"), new BigDecimal("40.00"), Instant.now(), "generated-id"));

        useCase.execute(EMAIL, new PlaceSimulatedOrderCommand("PETR4", SimulatedOrderSide.BUY, new BigDecimal("10"), null));

        // (10*30 + 10*40) / 20 = 35.00
        verify(simulatedPortfolioRepository).upsertPosition(
                eq(PORTFOLIO_ID), eq("PETR4"), eq(new BigDecimal("20")), eq(new BigDecimal("35.00")));
    }

    @Test
    void execute_Sell_WithSufficientPosition_CreditsBalanceAndReducesPosition() {
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL))
                .thenReturn(portfolioWithBalance(new BigDecimal("1000.00")));
        when(externalInvestmentApiPort.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 30.00, "BRL")));
        when(simulatedPortfolioRepository.findOrderByClientOrderId(eq(PORTFOLIO_ID), anyString()))
                .thenReturn(Optional.empty());
        when(simulatedPortfolioRepository.findPosition(PORTFOLIO_ID, "PETR4"))
                .thenReturn(Optional.of(new SimulatedPosition(1L, PORTFOLIO_ID, "PETR4",
                        new BigDecimal("10"), new BigDecimal("25.00"))));
        when(simulatedPortfolioRepository.saveOrder(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new SimulatedOrder(2L, PORTFOLIO_ID, "PETR4", SimulatedOrderSide.SELL,
                        new BigDecimal("4"), new BigDecimal("30.00"), Instant.now(), "generated-id"));

        useCase.execute(EMAIL, new PlaceSimulatedOrderCommand("PETR4", SimulatedOrderSide.SELL, new BigDecimal("4"), null));

        verify(simulatedPortfolioRepository).updateBalance(PORTFOLIO_ID, new BigDecimal("1120.00"));
        verify(simulatedPortfolioRepository).upsertPosition(
                eq(PORTFOLIO_ID), eq("PETR4"), eq(new BigDecimal("6")), eq(new BigDecimal("25.00")));
        verify(simulatedPortfolioRepository, never()).deletePosition(any(), any());
    }

    @Test
    void execute_Sell_ThatClosesTheEntirePosition_DeletesIt() {
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL))
                .thenReturn(portfolioWithBalance(new BigDecimal("1000.00")));
        when(externalInvestmentApiPort.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 30.00, "BRL")));
        when(simulatedPortfolioRepository.findOrderByClientOrderId(eq(PORTFOLIO_ID), anyString()))
                .thenReturn(Optional.empty());
        when(simulatedPortfolioRepository.findPosition(PORTFOLIO_ID, "PETR4"))
                .thenReturn(Optional.of(new SimulatedPosition(1L, PORTFOLIO_ID, "PETR4",
                        new BigDecimal("10"), new BigDecimal("25.00"))));
        when(simulatedPortfolioRepository.saveOrder(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new SimulatedOrder(2L, PORTFOLIO_ID, "PETR4", SimulatedOrderSide.SELL,
                        new BigDecimal("10"), new BigDecimal("30.00"), Instant.now(), "generated-id"));

        useCase.execute(EMAIL, new PlaceSimulatedOrderCommand("PETR4", SimulatedOrderSide.SELL, new BigDecimal("10"), null));

        verify(simulatedPortfolioRepository).deletePosition(PORTFOLIO_ID, "PETR4");
        verify(simulatedPortfolioRepository, never()).upsertPosition(any(), any(), any(), any());
    }

    @Test
    void execute_Sell_WithNoPosition_Throws() {
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL))
                .thenReturn(portfolioWithBalance(new BigDecimal("1000.00")));
        when(externalInvestmentApiPort.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 30.00, "BRL")));
        when(simulatedPortfolioRepository.findOrderByClientOrderId(eq(PORTFOLIO_ID), anyString()))
                .thenReturn(Optional.empty());
        when(simulatedPortfolioRepository.findPosition(PORTFOLIO_ID, "PETR4")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL,
                new PlaceSimulatedOrderCommand("PETR4", SimulatedOrderSide.SELL, new BigDecimal("4"), null)));
    }

    @Test
    void execute_Sell_MoreThanHeld_Throws() {
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL))
                .thenReturn(portfolioWithBalance(new BigDecimal("1000.00")));
        when(externalInvestmentApiPort.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 30.00, "BRL")));
        when(simulatedPortfolioRepository.findOrderByClientOrderId(eq(PORTFOLIO_ID), anyString()))
                .thenReturn(Optional.empty());
        when(simulatedPortfolioRepository.findPosition(PORTFOLIO_ID, "PETR4"))
                .thenReturn(Optional.of(new SimulatedPosition(1L, PORTFOLIO_ID, "PETR4",
                        new BigDecimal("5"), new BigDecimal("25.00"))));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL,
                new PlaceSimulatedOrderCommand("PETR4", SimulatedOrderSide.SELL, new BigDecimal("10"), null)));
    }

    @Test
    void execute_WithUnknownTicker_Throws() {
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL))
                .thenReturn(portfolioWithBalance(new BigDecimal("1000.00")));
        when(externalInvestmentApiPort.getQuote("GHOST99")).thenReturn(Optional.empty());
        when(simulatedPortfolioRepository.findOrderByClientOrderId(eq(PORTFOLIO_ID), anyString()))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL,
                new PlaceSimulatedOrderCommand("GHOST99", SimulatedOrderSide.BUY, new BigDecimal("1"), null)));
    }

    @Test
    void execute_WithZeroOrNegativeQuantity_ThrowsBeforeFetchingAQuote() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL,
                new PlaceSimulatedOrderCommand("PETR4", SimulatedOrderSide.BUY, BigDecimal.ZERO, null)));

        verify(externalInvestmentApiPort, never()).getQuote(any());
    }

    @Test
    void execute_WithRepeatedClientOrderId_IsIdempotentAndNeverReExecutes() {
        SimulatedOrder existingOrder = new SimulatedOrder(1L, PORTFOLIO_ID, "PETR4", SimulatedOrderSide.BUY,
                new BigDecimal("10"), new BigDecimal("30.50"), Instant.now(), "retry-key");
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL))
                .thenReturn(portfolioWithBalance(new BigDecimal("10000.00")));
        when(simulatedPortfolioRepository.findOrderByClientOrderId(PORTFOLIO_ID, "retry-key"))
                .thenReturn(Optional.of(existingOrder));

        SimulatedOrderDTO result = useCase.execute(EMAIL,
                new PlaceSimulatedOrderCommand("PETR4", SimulatedOrderSide.BUY, new BigDecimal("10"), "retry-key"));

        assertEquals(existingOrder.id(), result.id());
        verify(externalInvestmentApiPort, never()).getQuote(any());
        verify(simulatedPortfolioRepository, never()).upsertPosition(any(), any(), any(), any());
        verify(simulatedPortfolioRepository, never()).updateBalance(any(), any());
        verify(simulatedPortfolioRepository, never()).saveOrder(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void execute_WithoutClientOrderId_GeneratesOneBeforeSaving() {
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL))
                .thenReturn(portfolioWithBalance(new BigDecimal("10000.00")));
        when(externalInvestmentApiPort.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 30.50, "BRL")));
        when(simulatedPortfolioRepository.findOrderByClientOrderId(eq(PORTFOLIO_ID), anyString()))
                .thenReturn(Optional.empty());
        when(simulatedPortfolioRepository.findPosition(PORTFOLIO_ID, "PETR4")).thenReturn(Optional.empty());
        when(simulatedPortfolioRepository.saveOrder(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new SimulatedOrder(1L, PORTFOLIO_ID, "PETR4", SimulatedOrderSide.BUY,
                        new BigDecimal("10"), new BigDecimal("30.50"), Instant.now(), "server-generated"));

        useCase.execute(EMAIL, new PlaceSimulatedOrderCommand("PETR4", SimulatedOrderSide.BUY, new BigDecimal("10"), null));

        ArgumentCaptor<String> clientOrderIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(simulatedPortfolioRepository, times(1)).saveOrder(
                eq(PORTFOLIO_ID), eq("PETR4"), eq(SimulatedOrderSide.BUY), any(), any(), any(), clientOrderIdCaptor.capture());
        assertEquals(36, clientOrderIdCaptor.getValue().length()); // UUID string length
    }

    private void stubPortfolioAndNoPriorOrder(BigDecimal balance) {
        when(getOrCreateSimulatedPortfolioUseCase.execute(EMAIL)).thenReturn(portfolioWithBalance(balance));
        when(simulatedPortfolioRepository.findOrderByClientOrderId(eq(PORTFOLIO_ID), anyString()))
                .thenReturn(Optional.empty());
    }

    private SimulatedOrder savedOrder(SimulatedOrderSide side, BigDecimal price, Instant executedAt) {
        return new SimulatedOrder(9L, PORTFOLIO_ID, "PETR4", side, new BigDecimal("10"), price, executedAt, "id");
    }

    @Test
    void execute_Buy_WithAPastTradeDate_UsesTheHistoricalCloseAndStampsTheOrderAtThatDate() {
        LocalDate tradeDate = LocalDate.now(ZoneOffset.UTC).minusMonths(6);
        stubPortfolioAndNoPriorOrder(new BigDecimal("10000.00"));
        when(externalInvestmentApiPort.getQuoteAtDate("PETR4", tradeDate))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 25.00, "BRL")));
        when(simulatedPortfolioRepository.findPosition(PORTFOLIO_ID, "PETR4")).thenReturn(Optional.empty());
        Instant expectedExecutedAt = tradeDate.atTime(12, 0).toInstant(ZoneOffset.UTC);
        when(simulatedPortfolioRepository.saveOrder(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(savedOrder(SimulatedOrderSide.BUY, new BigDecimal("25.00"), expectedExecutedAt));

        SimulatedOrderDTO result = useCase.execute(EMAIL, new PlaceSimulatedOrderCommand(
                "PETR4", SimulatedOrderSide.BUY, new BigDecimal("10"), null, tradeDate));

        verify(simulatedPortfolioRepository).saveOrder(eq(PORTFOLIO_ID), eq("PETR4"), eq(SimulatedOrderSide.BUY),
                eq(new BigDecimal("10")), eq(new BigDecimal("25.00")), eq(expectedExecutedAt), anyString());
        verify(simulatedPortfolioRepository).updateBalance(PORTFOLIO_ID, new BigDecimal("9750.00"));
        verify(externalInvestmentApiPort, never()).getQuote(any());
        assertEquals(expectedExecutedAt, result.executedAt());
    }

    @Test
    void execute_Buy_WithATradeDateOfToday_IsAnOrdinaryLiveOrder() {
        stubPortfolioAndNoPriorOrder(new BigDecimal("10000.00"));
        when(externalInvestmentApiPort.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 30.50, "BRL")));
        when(simulatedPortfolioRepository.findPosition(PORTFOLIO_ID, "PETR4")).thenReturn(Optional.empty());
        when(simulatedPortfolioRepository.saveOrder(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(savedOrder(SimulatedOrderSide.BUY, new BigDecimal("30.50"), Instant.now()));

        useCase.execute(EMAIL, new PlaceSimulatedOrderCommand(
                "PETR4", SimulatedOrderSide.BUY, new BigDecimal("10"), null, LocalDate.now(ZoneOffset.UTC)));

        verify(externalInvestmentApiPort, never()).getQuoteAtDate(any(), any());
        ArgumentCaptor<Instant> executedAt = ArgumentCaptor.forClass(Instant.class);
        verify(simulatedPortfolioRepository).saveOrder(any(), any(), any(), any(), any(), executedAt.capture(), any());
        assertTrue(Duration.between(executedAt.getValue(), Instant.now()).abs().getSeconds() < 5);
    }

    @Test
    void execute_WithAFutureTradeDate_ThrowsBeforeFetchingAnyQuote() {
        stubPortfolioAndNoPriorOrder(new BigDecimal("10000.00"));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL, new PlaceSimulatedOrderCommand(
                "PETR4", SimulatedOrderSide.BUY, new BigDecimal("10"), null, LocalDate.now(ZoneOffset.UTC).plusDays(1))));

        verify(externalInvestmentApiPort, never()).getQuote(any());
        verify(externalInvestmentApiPort, never()).getQuoteAtDate(any(), any());
        verify(simulatedPortfolioRepository, never()).saveOrder(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void execute_WithAPastTradeDateAndNoHistoricalQuote_ThrowsInsteadOfFallingBackToTheLivePrice() {
        LocalDate tradeDate = LocalDate.now(ZoneOffset.UTC).minusYears(30);
        stubPortfolioAndNoPriorOrder(new BigDecimal("10000.00"));
        when(externalInvestmentApiPort.getQuoteAtDate("PETR4", tradeDate)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL, new PlaceSimulatedOrderCommand(
                "PETR4", SimulatedOrderSide.BUY, new BigDecimal("10"), null, tradeDate)));

        verify(externalInvestmentApiPort, never()).getQuote(any());
        verify(simulatedPortfolioRepository, never()).saveOrder(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void execute_Sell_DatedBeforeTheFirstBuyOfThatTicker_Throws() {
        LocalDate firstBuy = LocalDate.now(ZoneOffset.UTC).minusMonths(2);
        LocalDate sellDate = firstBuy.minusDays(1);
        stubPortfolioAndNoPriorOrder(new BigDecimal("1000.00"));
        when(externalInvestmentApiPort.getQuoteAtDate("PETR4", sellDate))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 20.00, "BRL")));
        when(simulatedPortfolioRepository.findPosition(PORTFOLIO_ID, "PETR4"))
                .thenReturn(Optional.of(new SimulatedPosition(1L, PORTFOLIO_ID, "PETR4",
                        new BigDecimal("10"), new BigDecimal("25.00"))));
        when(simulatedPortfolioRepository.findOrders(PORTFOLIO_ID)).thenReturn(List.of(
                new SimulatedOrder(1L, PORTFOLIO_ID, "PETR4", SimulatedOrderSide.BUY, new BigDecimal("10"),
                        new BigDecimal("25.00"), firstBuy.atTime(12, 0).toInstant(ZoneOffset.UTC), "buy-1")));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL, new PlaceSimulatedOrderCommand(
                "PETR4", SimulatedOrderSide.SELL, new BigDecimal("4"), null, sellDate)));

        verify(simulatedPortfolioRepository, never()).updateBalance(any(), any());
        verify(simulatedPortfolioRepository, never()).saveOrder(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void execute_Sell_DatedOnOrAfterTheFirstBuy_UsesTheHistoricalCloseAndCreditsTheBalance() {
        LocalDate firstBuy = LocalDate.now(ZoneOffset.UTC).minusMonths(2);
        LocalDate sellDate = firstBuy.plusDays(10);
        stubPortfolioAndNoPriorOrder(new BigDecimal("1000.00"));
        when(externalInvestmentApiPort.getQuoteAtDate("PETR4", sellDate))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 30.00, "BRL")));
        when(simulatedPortfolioRepository.findPosition(PORTFOLIO_ID, "PETR4"))
                .thenReturn(Optional.of(new SimulatedPosition(1L, PORTFOLIO_ID, "PETR4",
                        new BigDecimal("10"), new BigDecimal("25.00"))));
        when(simulatedPortfolioRepository.findOrders(PORTFOLIO_ID)).thenReturn(List.of(
                new SimulatedOrder(1L, PORTFOLIO_ID, "PETR4", SimulatedOrderSide.BUY, new BigDecimal("10"),
                        new BigDecimal("25.00"), firstBuy.atTime(12, 0).toInstant(ZoneOffset.UTC), "buy-1")));
        Instant expectedExecutedAt = sellDate.atTime(12, 0).toInstant(ZoneOffset.UTC);
        when(simulatedPortfolioRepository.saveOrder(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(savedOrder(SimulatedOrderSide.SELL, new BigDecimal("30.00"), expectedExecutedAt));

        useCase.execute(EMAIL, new PlaceSimulatedOrderCommand(
                "PETR4", SimulatedOrderSide.SELL, new BigDecimal("4"), null, sellDate));

        verify(simulatedPortfolioRepository).updateBalance(PORTFOLIO_ID, new BigDecimal("1120.00"));
        verify(simulatedPortfolioRepository).saveOrder(eq(PORTFOLIO_ID), eq("PETR4"), eq(SimulatedOrderSide.SELL),
                eq(new BigDecimal("4")), eq(new BigDecimal("30.00")), eq(expectedExecutedAt), anyString());
    }

    @Test
    void execute_Sell_DatedBeforeTheFirstBuy_IgnoresEarlierOrdersOfOtherTickersAndSells() {
        LocalDate petrFirstBuy = LocalDate.now(ZoneOffset.UTC).minusMonths(2);
        LocalDate sellDate = petrFirstBuy.minusDays(5);
        Instant earlier = petrFirstBuy.minusMonths(1).atTime(12, 0).toInstant(ZoneOffset.UTC);
        stubPortfolioAndNoPriorOrder(new BigDecimal("1000.00"));
        when(externalInvestmentApiPort.getQuoteAtDate("PETR4", sellDate))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 20.00, "BRL")));
        when(simulatedPortfolioRepository.findPosition(PORTFOLIO_ID, "PETR4"))
                .thenReturn(Optional.of(new SimulatedPosition(1L, PORTFOLIO_ID, "PETR4",
                        new BigDecimal("10"), new BigDecimal("25.00"))));
        when(simulatedPortfolioRepository.findOrders(PORTFOLIO_ID)).thenReturn(List.of(
                new SimulatedOrder(1L, PORTFOLIO_ID, "VALE3", SimulatedOrderSide.BUY, new BigDecimal("5"),
                        new BigDecimal("60.00"), earlier, "other-ticker"),
                new SimulatedOrder(2L, PORTFOLIO_ID, "PETR4", SimulatedOrderSide.SELL, new BigDecimal("1"),
                        new BigDecimal("28.00"), earlier, "earlier-sell"),
                new SimulatedOrder(3L, PORTFOLIO_ID, "PETR4", SimulatedOrderSide.BUY, new BigDecimal("10"),
                        new BigDecimal("25.00"), petrFirstBuy.atTime(12, 0).toInstant(ZoneOffset.UTC), "buy-1")));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(EMAIL, new PlaceSimulatedOrderCommand(
                "PETR4", SimulatedOrderSide.SELL, new BigDecimal("4"), null, sellDate)));
    }

    @Test
    void execute_Buy_ThatCostsExactlyTheWholeBalance_IsAllowed() {
        stubPortfolioAndNoPriorOrder(new BigDecimal("305.00"));
        when(externalInvestmentApiPort.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 30.50, "BRL")));
        when(simulatedPortfolioRepository.findPosition(PORTFOLIO_ID, "PETR4")).thenReturn(Optional.empty());
        when(simulatedPortfolioRepository.saveOrder(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(savedOrder(SimulatedOrderSide.BUY, new BigDecimal("30.50"), Instant.now()));

        useCase.execute(EMAIL,
                new PlaceSimulatedOrderCommand("PETR4", SimulatedOrderSide.BUY, new BigDecimal("10"), null));

        verify(simulatedPortfolioRepository).updateBalance(PORTFOLIO_ID, new BigDecimal("0.00"));
    }
}
