package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.InvestmentLotDTO;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import com.jf.PetApp.core.domain.enums.PriceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class GetPortfolioSummaryUseCaseImplTest {

    @Mock
    private GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase;

    @InjectMocks
    private GetPortfolioSummaryUseCaseImpl getPortfolioSummaryUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }

    /** Compares a plain double to a BigDecimal money field by value, ignoring scale. */
    private static void assertMoney(double expected, BigDecimal actual) {
        assertEquals(0, bd(expected).setScale(2, RoundingMode.HALF_UP).compareTo(actual));
    }

    @Test
    void execute_WithTwoLots_ShouldComputeInvestedCurrentGainAndPercent() {
        String email = "investor@test.com";

        // Lot 1: invested 100 * 10 = 1000, current 100 * 12 = 1200
        InvestmentLotDTO lot1 = new InvestmentLotDTO(
                1, "PETR4", InvestmentType.STOCKS, bd(100.0), bd(10.0), LocalDate.now(),
                bd(12.0), bd(1000.0), bd(1200.0), PriceStatus.LIVE);

        // Lot 2: invested 10 * 50 = 500, current 10 * 40 = 400
        InvestmentLotDTO lot2 = new InvestmentLotDTO(
                2, "VALE3", InvestmentType.STOCKS, bd(10.0), bd(50.0), LocalDate.now(),
                bd(40.0), bd(500.0), bd(400.0), PriceStatus.LIVE);

        when(getPortfolioHoldingsUseCase.execute(email)).thenReturn(List.of(lot1, lot2));

        var summary = getPortfolioSummaryUseCase.execute(email);

        // investedCapital = 1000 + 500 = 1500
        // currentValue = 1200 + 400 = 1600
        // totalGain = 1600 - 1500 = 100
        // totalGainPercent = 100 / 1500 * 100 = 6.666...
        assertMoney(1500.0, summary.investedCapital());
        assertMoney(1600.0, summary.currentValue());
        assertMoney(100.0, summary.totalGain());
        assertMoney(100.0 / 1500.0 * 100, summary.totalGainPercent());
        assertEquals(2, summary.totalAssets());
    }

    @Test
    void execute_WithNoLots_ShouldReturnZeroedSummaryWithoutDivisionByZero() {
        String email = "empty@test.com";

        when(getPortfolioHoldingsUseCase.execute(email)).thenReturn(List.of());

        var summary = getPortfolioSummaryUseCase.execute(email);

        assertMoney(0.0, summary.investedCapital());
        assertMoney(0.0, summary.currentValue());
        assertMoney(0.0, summary.totalGain());
        assertMoney(0.0, summary.totalGainPercent());
        assertEquals(0, summary.totalAssets());
    }
}
