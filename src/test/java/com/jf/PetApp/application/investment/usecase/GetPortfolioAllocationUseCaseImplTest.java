package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.AllocationSliceDTO;
import com.jf.PetApp.application.investment.dto.InvestmentLotDTO;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class GetPortfolioAllocationUseCaseImplTest {

    @Mock
    private GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase;

    private GetPortfolioAllocationUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetPortfolioAllocationUseCaseImpl(getPortfolioHoldingsUseCase);
    }

    private InvestmentLotDTO lot(InvestmentType type, double currentValue) {
        BigDecimal value = BigDecimal.valueOf(currentValue);
        return new InvestmentLotDTO(1, "X", type, BigDecimal.ONE, value, LocalDate.now(), value, value, value);
    }

    /** Compares a plain double to a BigDecimal money/percent field by value, ignoring scale. */
    private static void assertMoney(double expected, BigDecimal actual) {
        assertEquals(0, BigDecimal.valueOf(expected).setScale(2, RoundingMode.HALF_UP).compareTo(actual));
    }

    @Test
    void execute_WithNoHoldings_ReturnsEmptyAllocation() {
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of());

        List<AllocationSliceDTO> result = useCase.execute(EMAIL);

        assertTrue(result.isEmpty());
    }

    @Test
    void execute_WithSingleType_Allocates100Percent() {
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of(lot(InvestmentType.STOCKS, 1000.0)));

        List<AllocationSliceDTO> result = useCase.execute(EMAIL);

        assertEquals(1, result.size());
        assertEquals(InvestmentType.STOCKS, result.get(0).type());
        assertMoney(1000.0, result.get(0).currentValue());
        assertMoney(100.0, result.get(0).portfolioPercent());
    }

    @Test
    void execute_WithTwoTypes_PercentagesSumTo100() {
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of(
                lot(InvestmentType.STOCKS, 750.0),
                lot(InvestmentType.FIXED_INCOME, 250.0)
        ));

        List<AllocationSliceDTO> result = useCase.execute(EMAIL);

        BigDecimal totalPercent = result.stream().map(AllocationSliceDTO::portfolioPercent).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertMoney(100.0, totalPercent);

        AllocationSliceDTO stocks = result.stream().filter(s -> s.type() == InvestmentType.STOCKS).findFirst().orElseThrow();
        assertMoney(75.0, stocks.portfolioPercent());
    }

    @Test
    void execute_GroupsMultipleLotsOfTheSameTypeTogether() {
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of(
                lot(InvestmentType.STOCKS, 300.0),
                lot(InvestmentType.STOCKS, 700.0)
        ));

        List<AllocationSliceDTO> result = useCase.execute(EMAIL);

        assertEquals(1, result.size());
        assertMoney(1000.0, result.get(0).currentValue());
        assertMoney(100.0, result.get(0).portfolioPercent());
    }

    @Test
    void execute_WithZeroTotalCurrentValue_NeverDividesByZero() {
        when(getPortfolioHoldingsUseCase.execute(EMAIL)).thenReturn(List.of(lot(InvestmentType.CRYPTO, 0.0)));

        List<AllocationSliceDTO> result = useCase.execute(EMAIL);

        assertMoney(0.0, result.get(0).portfolioPercent());
    }
}
