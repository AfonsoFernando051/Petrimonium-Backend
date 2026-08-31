package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.InvestmentLotDTO;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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

    @Test
    void execute_WithTwoLots_ShouldComputeInvestedCurrentGainAndPercent() {
        String email = "investor@test.com";

        // Lot 1: invested 100 * 10 = 1000, current 100 * 12 = 1200
        InvestmentLotDTO lot1 = new InvestmentLotDTO(
                1, "PETR4", InvestmentType.STOCKS, 100.0, 10.0, LocalDate.now(),
                12.0, 1000.0, 1200.0);

        // Lot 2: invested 10 * 50 = 500, current 10 * 40 = 400
        InvestmentLotDTO lot2 = new InvestmentLotDTO(
                2, "VALE3", InvestmentType.STOCKS, 10.0, 50.0, LocalDate.now(),
                40.0, 500.0, 400.0);

        when(getPortfolioHoldingsUseCase.execute(email)).thenReturn(List.of(lot1, lot2));

        var summary = getPortfolioSummaryUseCase.execute(email);

        // investedCapital = 1000 + 500 = 1500
        // currentValue = 1200 + 400 = 1600
        // totalGain = 1600 - 1500 = 100
        // totalGainPercent = 100 / 1500 * 100 = 6.666...
        assertEquals(1500.0, summary.investedCapital());
        assertEquals(1600.0, summary.currentValue());
        assertEquals(100.0, summary.totalGain(), 0.0001);
        assertEquals(100.0 / 1500.0 * 100, summary.totalGainPercent(), 0.0001);
        assertEquals(2, summary.totalAssets());
    }

    @Test
    void execute_WithNoLots_ShouldReturnZeroedSummaryWithoutDivisionByZero() {
        String email = "empty@test.com";

        when(getPortfolioHoldingsUseCase.execute(email)).thenReturn(List.of());

        var summary = getPortfolioSummaryUseCase.execute(email);

        assertEquals(0.0, summary.investedCapital());
        assertEquals(0.0, summary.currentValue());
        assertEquals(0.0, summary.totalGain());
        assertEquals(0.0, summary.totalGainPercent());
        assertEquals(0, summary.totalAssets());
    }
}
