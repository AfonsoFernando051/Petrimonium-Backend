package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.DividendDTO;
import com.jf.PetApp.application.investment.dto.DividendRadarEntryDTO;
import com.jf.PetApp.application.investment.dto.DividendRadarResponseDTO;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;
import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import com.jf.PetApp.core.domain.Investment;
import com.jf.PetApp.core.domain.enums.DividendType;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class GetDividendRadarUseCaseImplTest {

    @Mock
    private InvestmentRepositoryPort investmentRepositoryPort;

    @Mock
    private ExternalInvestmentApiPort externalInvestmentApiPort;

    private GetDividendRadarUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";
    private static final LocalDate TODAY = LocalDate.now();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetDividendRadarUseCaseImpl(investmentRepositoryPort, externalInvestmentApiPort);
    }

    private Investment lot(String ticker, double quantity, LocalDate purchaseDate) {
        return new Investment(1, EMAIL, ticker, quantity, 10.0, purchaseDate, InvestmentType.STOCKS);
    }

    private DividendDTO dividend(String ticker, LocalDate dataCom, LocalDate paymentDate) {
        return new DividendDTO(ticker, DividendType.DIVIDENDO, "Dividendo", 1.5, dataCom, paymentDate, dataCom);
    }

    @Test
    void execute_WithNoHoldings_ReturnsEmptyRadar() {
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of());

        DividendRadarResponseDTO result = useCase.execute(EMAIL);

        assertTrue(result.upcoming().isEmpty());
        assertTrue(result.history().isEmpty());
        verifyNoInteractions(externalInvestmentApiPort);
    }

    @Test
    void execute_WithZeroCurrentQuantity_SkipsTickerEntirely() {
        // Fully sold position: a buy lot and an equal-and-opposite sale would
        // both be persisted as lots in a real ledger; here we simulate net-zero
        // by using a single lot with zero quantity.
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of(lot("PETR4", 0.0, TODAY.minusDays(30))));

        DividendRadarResponseDTO result = useCase.execute(EMAIL);

        assertTrue(result.upcoming().isEmpty());
        assertTrue(result.history().isEmpty());
        verifyNoInteractions(externalInvestmentApiPort);
    }

    @Test
    void execute_WithFuturePaymentDate_GoesToUpcomingScaledByCurrentQuantity() {
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of(lot("PETR4", 100.0, TODAY.minusDays(30))));
        when(externalInvestmentApiPort.getDividends("PETR4"))
                .thenReturn(List.of(dividend("PETR4", TODAY.minusDays(5), TODAY.plusDays(10))));

        DividendRadarResponseDTO result = useCase.execute(EMAIL);

        assertEquals(1, result.upcoming().size());
        assertTrue(result.history().isEmpty());
        DividendRadarEntryDTO entry = result.upcoming().get(0);
        assertEquals(DividendRadarEntryDTO.STATUS_ANNOUNCED, entry.status());
        assertEquals(100.0, entry.userQuantity());
        assertEquals(150.0, entry.estimatedGrossAmount()); // 1.5 * 100
    }

    @Test
    void execute_WithPastPaymentDateAndHeldSinceBeforeDataCom_GoesToHistory() {
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of(lot("PETR4", 100.0, TODAY.minusDays(60))));
        when(externalInvestmentApiPort.getDividends("PETR4"))
                .thenReturn(List.of(dividend("PETR4", TODAY.minusDays(20), TODAY.minusDays(5))));

        DividendRadarResponseDTO result = useCase.execute(EMAIL);

        assertTrue(result.upcoming().isEmpty());
        assertEquals(1, result.history().size());
        DividendRadarEntryDTO entry = result.history().get(0);
        assertEquals(DividendRadarEntryDTO.STATUS_PAID, entry.status());
        assertEquals(100.0, entry.userQuantity());
    }

    @Test
    void execute_WithPaidDividendButPurchasedAfterDataCom_ExcludesFromHistory() {
        // Bought 5 days ago, but the data-com (eligibility cutoff) was 10 days
        // ago — this position never qualified for the payment, so scaling by
        // today's quantity would fabricate a payment the user never received.
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of(lot("PETR4", 100.0, TODAY.minusDays(5))));
        when(externalInvestmentApiPort.getDividends("PETR4"))
                .thenReturn(List.of(dividend("PETR4", TODAY.minusDays(10), TODAY.minusDays(2))));

        DividendRadarResponseDTO result = useCase.execute(EMAIL);

        assertTrue(result.history().isEmpty());
        assertTrue(result.upcoming().isEmpty());
    }

    @Test
    void execute_WithPaidDividendAndNullDataCom_FallsBackToCurrentQuantity() {
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of(lot("PETR4", 50.0, TODAY.minusDays(60))));
        when(externalInvestmentApiPort.getDividends("PETR4"))
                .thenReturn(List.of(dividend("PETR4", null, TODAY.minusDays(2))));

        DividendRadarResponseDTO result = useCase.execute(EMAIL);

        assertEquals(1, result.history().size());
        assertEquals(50.0, result.history().get(0).userQuantity());
    }

    @Test
    void execute_WhenProviderThrowsForATicker_SkipsItWithoutCrashing() {
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of(lot("PETR4", 100.0, TODAY.minusDays(30))));
        when(externalInvestmentApiPort.getDividends("PETR4")).thenThrow(new RuntimeException("provider down"));

        DividendRadarResponseDTO result = useCase.execute(EMAIL);

        assertTrue(result.upcoming().isEmpty());
        assertTrue(result.history().isEmpty());
    }

    @Test
    void execute_UpcomingIsSortedBySoonestPaymentDateFirst() {
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of(lot("PETR4", 100.0, TODAY.minusDays(30))));
        when(externalInvestmentApiPort.getDividends("PETR4")).thenReturn(List.of(
                dividend("PETR4", TODAY.minusDays(1), TODAY.plusDays(30)),
                dividend("PETR4", TODAY.minusDays(1), TODAY.plusDays(5))
        ));

        List<DividendRadarEntryDTO> upcoming = useCase.execute(EMAIL).upcoming();

        assertEquals(TODAY.plusDays(5), upcoming.get(0).paymentDate());
        assertEquals(TODAY.plusDays(30), upcoming.get(1).paymentDate());
    }

    @Test
    void execute_HistoryIsBoundedPerTicker() {
        when(investmentRepositoryPort.findByUserEmail(EMAIL)).thenReturn(List.of(lot("PETR4", 100.0, TODAY.minusYears(5))));
        List<DividendDTO> manyPayments = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            manyPayments.add(dividend("PETR4", TODAY.minusDays(400L + i), TODAY.minusDays(390L + i)));
        }
        when(externalInvestmentApiPort.getDividends("PETR4")).thenReturn(manyPayments);

        List<DividendRadarEntryDTO> history = useCase.execute(EMAIL).history();

        assertEquals(12, history.size()); // MAX_HISTORY_PER_TICKER
    }
}
