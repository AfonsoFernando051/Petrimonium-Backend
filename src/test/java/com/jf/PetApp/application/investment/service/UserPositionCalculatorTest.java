package com.jf.PetApp.application.investment.service;

import com.jf.PetApp.application.investment.dto.UserPositionDTO;
import com.jf.PetApp.core.domain.Investment;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserPositionCalculatorTest {

    private static final double DELTA = 0.0001;

    private final UserPositionCalculator calculator = new UserPositionCalculator();

    private Investment lot(String ticker, double quantity, double purchasePrice) {
        return new Investment(1, "investor@test.com", ticker, quantity, purchasePrice, LocalDate.now(), InvestmentType.STOCKS);
    }

    @Test
    void compute_UserHoldsNoneOfTheTicker_ReturnsNull() {
        UserPositionDTO result = calculator.compute(List.of(lot("VALE3", 10, 60.0)), "PETR4", 30.0);

        assertNull(result);
    }

    @Test
    void compute_SingleLot_ComputesAveragePriceAndUnrealizedGain() {
        UserPositionDTO result = calculator.compute(List.of(lot("PETR4", 100, 30.0)), "PETR4", 40.0);

        assertEquals(100, result.quantity(), DELTA);
        assertEquals(30.0, result.averagePrice(), DELTA);
        assertEquals(3000.0, result.investedValue(), DELTA);
        assertEquals(4000.0, result.currentValue(), DELTA);
        assertEquals(1000.0, result.unrealizedGain(), DELTA);
        assertEquals(100.0, result.portfolioWeight(), DELTA); // only holding
    }

    @Test
    void compute_MultipleLotsOfSameTicker_WeightsTheAveragePriceByQuantity() {
        List<Investment> lots = List.of(
            lot("PETR4", 100, 20.0),
            lot("PETR4", 100, 40.0)
        );

        UserPositionDTO result = calculator.compute(lots, "PETR4", 30.0);

        assertEquals(200, result.quantity(), DELTA);
        assertEquals(30.0, result.averagePrice(), DELTA); // (2000 + 4000) / 200
        assertEquals(0.0, result.unrealizedGain(), DELTA); // current price == average price
    }

    @Test
    void compute_NoCurrentPrice_FallsBackToAveragePurchasePriceWithZeroGain() {
        UserPositionDTO result = calculator.compute(List.of(lot("PETR4", 10, 25.0)), "PETR4", null);

        assertEquals(25.0, result.averagePrice(), DELTA);
        assertEquals(250.0, result.currentValue(), DELTA);
        assertEquals(0.0, result.unrealizedGain(), DELTA);
    }

    @Test
    void compute_MultipleTickers_PortfolioWeightReflectsShareOfTotalValue() {
        List<Investment> lots = List.of(
            lot("PETR4", 100, 30.0), // this ticker: 100 * 40 (live) = 4000
            lot("VALE3", 50, 60.0)   // other holding priced at purchase price: 50 * 60 = 3000
        );

        UserPositionDTO result = calculator.compute(lots, "PETR4", 40.0);

        // total portfolio value = 4000 + 3000 = 7000; this position = 4000 / 7000
        assertEquals(4000.0 / 7000.0 * 100, result.portfolioWeight(), DELTA);
    }

    @Test
    void compute_TickerMatchIsCaseInsensitive() {
        UserPositionDTO result = calculator.compute(List.of(lot("petr4", 10, 20.0)), "PETR4", 20.0);

        assertEquals(10, result.quantity(), DELTA);
    }
}
