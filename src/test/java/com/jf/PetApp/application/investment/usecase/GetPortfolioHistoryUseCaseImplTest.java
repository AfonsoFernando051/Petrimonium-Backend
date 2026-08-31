package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.InvestmentLotDTO;
import com.jf.PetApp.application.investment.dto.PortfolioHistoryPointDTO;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GetPortfolioHistoryUseCaseImplTest {

    @Mock
    private GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase;

    private GetPortfolioHistoryUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";
    private static final LocalDate TODAY = LocalDate.now();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetPortfolioHistoryUseCaseImpl(getPortfolioHoldingsUseCase);
    }

    private InvestmentLotDTO lot(double quantity, double purchasePrice, LocalDate purchaseDate, double currentPrice) {
        BigDecimal q = BigDecimal.valueOf(quantity);
        BigDecimal purchase = BigDecimal.valueOf(purchasePrice);
        BigDecimal current = BigDecimal.valueOf(currentPrice);
        return new InvestmentLotDTO(
                1, "PETR4", InvestmentType.STOCKS, q, purchase, purchaseDate,
                current, q.multiply(purchase), q.multiply(current)
        );
    }

    /** Compares a plain double to a BigDecimal money field by value, ignoring scale. */
    private static void assertMoney(double expected, BigDecimal actual) {
        assertEquals(0, BigDecimal.valueOf(expected).setScale(2, RoundingMode.HALF_UP).compareTo(actual));
    }

    /** Same as {@link #assertMoney}, but within an absolute tolerance. */
    private static void assertMoneyApprox(double expected, BigDecimal actual, double delta) {
        BigDecimal diff = BigDecimal.valueOf(expected).subtract(actual).abs();
        assertTrue(diff.compareTo(BigDecimal.valueOf(delta)) <= 0,
                () -> "expected " + expected + " within " + delta + " of " + actual);
    }

    @Test
    void execute_WithNoLots_ReturnsASinglePointAtTodayWithZeroValue() {
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of());

        List<PortfolioHistoryPointDTO> points = useCase.execute(EMAIL, "ALL");

        assertEquals(1, points.size());
        assertEquals(TODAY, points.get(0).date());
        assertMoney(0.0, points.get(0).investedCapital());
        assertMoney(0.0, points.get(0).portfolioValue());
    }

    @Test
    void execute_With7DRange_ProducesOneSamplePerDay() {
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of(lot(100, 10, TODAY.minusDays(5), 12)));

        List<PortfolioHistoryPointDTO> points = useCase.execute(EMAIL, "7D");

        assertEquals(8, points.size()); // today-7..today inclusive
        assertEquals(TODAY.minusDays(7), points.get(0).date());
        assertEquals(TODAY, points.get(points.size() - 1).date());
    }

    @Test
    void execute_LotPurchasedToday_HasFullProgressImmediately() {
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of(lot(10, 10, TODAY, 20)));

        List<PortfolioHistoryPointDTO> points = useCase.execute(EMAIL, "7D");
        PortfolioHistoryPointDTO todayPoint = points.get(points.size() - 1);

        // Bought today at 10, now worth 20 -> today's sample should already
        // reflect the full current value, not a part-way interpolation.
        assertMoney(100.0, todayPoint.investedCapital()); // 10 * 10
        assertMoney(200.0, todayPoint.portfolioValue());  // 10 * 20
    }

    @Test
    void execute_SamplesBeforePurchaseDateExcludeThatLot() {
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of(lot(10, 10, TODAY.minusDays(3), 20)));

        List<PortfolioHistoryPointDTO> points = useCase.execute(EMAIL, "7D");
        PortfolioHistoryPointDTO firstPoint = points.get(0); // today - 7, before the purchase

        assertMoney(0.0, firstPoint.investedCapital());
        assertMoney(0.0, firstPoint.portfolioValue());
    }

    @Test
    void execute_MidwaySampleInterpolatesLinearlyBetweenPurchaseAndCurrentPrice() {
        // Bought 10 days ago at price 10, now worth 20 (linear path assumed).
        // The sample exactly 5 days in (halfway through the 10-day holding
        // period) should be halfway between 10 and 20 -> 15.
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of(lot(1, 10, TODAY.minusDays(10), 20)));

        List<PortfolioHistoryPointDTO> points = useCase.execute(EMAIL, "ALL");
        PortfolioHistoryPointDTO midpoint = points.stream()
                .filter(p -> p.date().equals(TODAY.minusDays(5)))
                .findFirst()
                .orElseThrow();

        assertMoneyApprox(15.0, midpoint.portfolioValue(), 0.5);
    }

    @Test
    void execute_UnknownRange_DefaultsToEarliestPurchaseDate() {
        LocalDate earliest = TODAY.minusDays(40);
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of(
                lot(10, 10, earliest, 12),
                lot(5, 20, TODAY.minusDays(10), 22)
        ));

        List<PortfolioHistoryPointDTO> points = useCase.execute(EMAIL, "not-a-real-range");

        assertEquals(earliest, points.get(0).date());
        assertEquals(TODAY, points.get(points.size() - 1).date());
    }

    @Test
    void execute_NullRange_DefaultsToAllTime() {
        LocalDate earliest = TODAY.minusDays(15);
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of(lot(10, 10, earliest, 12)));

        List<PortfolioHistoryPointDTO> points = useCase.execute(EMAIL, null);

        assertEquals(earliest, points.get(0).date());
    }

    @Test
    void execute_MultipleLots_SumsContributionsPerSample() {
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of(
                lot(10, 10, TODAY.minusDays(30), 10), // flat, no gain
                lot(5, 20, TODAY.minusDays(30), 20)    // flat, no gain
        ));

        List<PortfolioHistoryPointDTO> points = useCase.execute(EMAIL, "7D");
        PortfolioHistoryPointDTO todayPoint = points.get(points.size() - 1);

        assertMoney(100.0 + 100.0, todayPoint.investedCapital()); // 10*10 + 5*20
        assertMoney(100.0 + 100.0, todayPoint.portfolioValue());
    }

    @Test
    void execute_LongRangeCapsSampleCountAtSixty() {
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of(lot(10, 10, TODAY.minusYears(5), 12)));

        List<PortfolioHistoryPointDTO> points = useCase.execute(EMAIL, "5Y");

        assertTrue(points.size() <= 60);
        assertEquals(TODAY, points.get(points.size() - 1).date());
    }
}
