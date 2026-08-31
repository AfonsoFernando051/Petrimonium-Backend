package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.dto.InvestmentLotDTO;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;
import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import com.jf.PetApp.core.domain.Investment;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GetPortfolioHoldingsUseCaseImplTest {

    @Mock
    private InvestmentRepositoryPort investmentRepositoryPort;

    @Mock
    private ExternalInvestmentApiPort externalInvestmentApiPort;

    private GetPortfolioHoldingsUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetPortfolioHoldingsUseCaseImpl(investmentRepositoryPort, externalInvestmentApiPort);
    }

    private Investment lot(Integer id, String ticker, double quantity, double purchasePrice) {
        return new Investment(id, EMAIL, ticker, BigDecimal.valueOf(quantity), BigDecimal.valueOf(purchasePrice), LocalDate.now(), InvestmentType.STOCKS);
    }

    /** Compares a plain double to a BigDecimal money field by value, ignoring scale
     * (e.g. {@code 35.0} vs the production code's {@code 35.00}). */
    private static void assertMoney(double expected, BigDecimal actual) {
        assertEquals(0, BigDecimal.valueOf(expected).setScale(2, RoundingMode.HALF_UP).compareTo(actual));
    }

    @Test
    void execute_WithNoHoldings_ReturnsEmptyList() {
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of());

        List<InvestmentLotDTO> result = useCase.execute(EMAIL);

        assertEquals(0, result.size());
        verifyNoInteractions(externalInvestmentApiPort);
    }

    @Test
    void execute_WhenQuoteAvailable_UsesLiveMarketPriceForCurrentValue() {
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of(lot(1, "PETR4", 100.0, 30.0)));
        when(externalInvestmentApiPort.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 35.0, "BRL")));

        InvestmentLotDTO result = useCase.execute(EMAIL).get(0);

        assertMoney(35.0, result.currentPrice());
        assertMoney(3000.0, result.investedValue()); // 100 * 30
        assertMoney(3500.0, result.currentValue());  // 100 * 35
    }

    @Test
    void execute_WhenQuoteMissing_FallsBackToPurchasePrice() {
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of(lot(1, "UNKNOWN4", 10.0, 50.0)));
        when(externalInvestmentApiPort.getQuote("UNKNOWN4")).thenReturn(Optional.empty());

        InvestmentLotDTO result = useCase.execute(EMAIL).get(0);

        assertMoney(50.0, result.currentPrice());
        assertMoney(500.0, result.investedValue());
        assertMoney(500.0, result.currentValue());
    }

    @Test
    void execute_WhenQuoteHasNullPrice_FallsBackToPurchasePrice() {
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of(lot(1, "PETR4", 10.0, 30.0)));
        when(externalInvestmentApiPort.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", null, "BRL")));

        InvestmentLotDTO result = useCase.execute(EMAIL).get(0);

        assertMoney(30.0, result.currentPrice());
    }

    @Test
    void execute_WhenProviderThrows_FallsBackToPurchasePriceWithoutPropagating() {
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of(lot(1, "PETR4", 10.0, 30.0)));
        when(externalInvestmentApiPort.getQuote("PETR4")).thenThrow(new RuntimeException("provider down"));

        InvestmentLotDTO result = useCase.execute(EMAIL).get(0);

        assertMoney(30.0, result.currentPrice());
    }

    @Test
    void execute_WithMultipleLotsOfSameTicker_FetchesQuoteOnlyOnce() {
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of(
                lot(1, "PETR4", 10.0, 30.0),
                lot(2, "PETR4", 5.0, 32.0)
        ));
        when(externalInvestmentApiPort.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 35.0, "BRL")));

        List<InvestmentLotDTO> result = useCase.execute(EMAIL);

        assertEquals(2, result.size());
        assertMoney(35.0, result.get(0).currentPrice());
        assertMoney(35.0, result.get(1).currentPrice());
        verify(externalInvestmentApiPort, times(1)).getQuote("PETR4");
    }

    @Test
    void execute_PreservesLotIdentityFields() {
        LocalDate purchaseDate = LocalDate.of(2024, 3, 15);
        Investment investment = new Investment(7, EMAIL, "VALE3", BigDecimal.valueOf(20.0), BigDecimal.valueOf(60.0), purchaseDate, InvestmentType.STOCKS);
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of(investment));
        when(externalInvestmentApiPort.getQuote("VALE3")).thenReturn(Optional.empty());

        InvestmentLotDTO result = useCase.execute(EMAIL).get(0);

        assertEquals(7, result.id());
        assertEquals("VALE3", result.name());
        assertEquals(InvestmentType.STOCKS, result.type());
        assertEquals(purchaseDate, result.purchaseDate());
    }
}
