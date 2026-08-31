package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.DividendDTO;
import com.jf.PetApp.application.investment.dto.DividendRadarEntryDTO;
import com.jf.PetApp.application.investment.dto.DividendRadarResponseDTO;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;
import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import com.jf.PetApp.core.domain.Investment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the "Dividend Radar" — the real, confirmed corporate-action history
 * and announcements for every ticker the user actually holds, each entry
 * scaled by the user's real quantity. Unlike {@code PassiveIncomeEstimate}
 * (mobile-side, projected from an assumed yield per asset category), every
 * number here traces back to a provider-confirmed payment; a ticker the
 * provider has nothing to report for simply contributes nothing, it is never
 * backfilled with a guess.
 */
@Service
public class GetDividendRadarUseCaseImpl implements GetDividendRadarUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetDividendRadarUseCaseImpl.class);

    /** Bounds payload size for long-lived tickers with decades of history (e.g. PETR4). */
    private static final int MAX_HISTORY_PER_TICKER = 12;

    private final InvestmentRepositoryPort investmentRepositoryPort;
    private final ExternalInvestmentApiPort externalInvestmentApiPort;

    public GetDividendRadarUseCaseImpl(InvestmentRepositoryPort investmentRepositoryPort,
                                        ExternalInvestmentApiPort externalInvestmentApiPort) {
        this.investmentRepositoryPort = investmentRepositoryPort;
        this.externalInvestmentApiPort = externalInvestmentApiPort;
    }

    @Override
    public DividendRadarResponseDTO execute(String email) {
        List<Investment> lots = investmentRepositoryPort.findByUserEmail(email);

        Map<String, List<Investment>> lotsByTicker = lots.stream()
                .collect(Collectors.groupingBy(Investment::name));

        LocalDate today = LocalDate.now();
        List<DividendRadarEntryDTO> upcoming = new ArrayList<>();
        List<DividendRadarEntryDTO> history = new ArrayList<>();

        for (Map.Entry<String, List<Investment>> holding : lotsByTicker.entrySet()) {
            String ticker = holding.getKey();
            List<Investment> tickerLots = holding.getValue();
            double currentQuantity = tickerLots.stream().mapToDouble(Investment::quantity).sum();
            if (currentQuantity <= 0) continue;

            List<DividendDTO> dividends = fetchDividends(ticker);
            for (DividendDTO dividend : dividends) {
                boolean paid = dividend.paymentDate() != null && !dividend.paymentDate().isAfter(today);

                if (paid) {
                    // A payment already made only belongs in "what you received" if the
                    // user actually held the position by the data-com date — otherwise
                    // scaling by today's quantity would fabricate a payment they never
                    // got (e.g. a position bought after this dividend's data-com).
                    double heldAsOfDataCom = quantityHeldAsOf(tickerLots, dividend.dataCom(), currentQuantity);
                    if (heldAsOfDataCom <= 0) continue;
                    history.add(toEntry(dividend, heldAsOfDataCom, DividendRadarEntryDTO.STATUS_PAID));
                } else {
                    // Forward-looking: scaled by the current position, the best available
                    // estimate of what the user will hold by the (not yet reached) payment date.
                    upcoming.add(toEntry(dividend, currentQuantity, DividendRadarEntryDTO.STATUS_ANNOUNCED));
                }
            }
        }

        upcoming.sort(Comparator.comparing(DividendRadarEntryDTO::paymentDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        history.sort(Comparator.comparing(DividendRadarEntryDTO::paymentDate,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        List<DividendRadarEntryDTO> boundedHistory = boundPerTicker(history, MAX_HISTORY_PER_TICKER);

        return new DividendRadarResponseDTO(upcoming, boundedHistory);
    }

    /**
     * Sums quantity across lots purchased on or before {@code dataCom} (the
     * last date a holder qualified for that payment). Falls back to the
     * user's current total quantity only when the provider didn't report a
     * data-com date at all — an honest best effort, not a fabrication, since
     * there's no eligibility cutoff to check the purchase history against.
     */
    private double quantityHeldAsOf(List<Investment> tickerLots, LocalDate dataCom, double currentQuantity) {
        if (dataCom == null) return currentQuantity;
        return tickerLots.stream()
                .filter(lot -> lot.purchaseDate() != null && !lot.purchaseDate().isAfter(dataCom))
                .mapToDouble(Investment::quantity)
                .sum();
    }

    private DividendRadarEntryDTO toEntry(DividendDTO dividend, double quantity, String status) {
        return new DividendRadarEntryDTO(
                dividend.ticker(),
                dividend.type(),
                dividend.rawLabel(),
                dividend.ratePerShare(),
                dividend.dataCom(),
                dividend.paymentDate(),
                dividend.approvedOn(),
                quantity,
                dividend.ratePerShare() * quantity,
                status
        );
    }

    private List<DividendDTO> fetchDividends(String ticker) {
        try {
            return externalInvestmentApiPort.getDividends(ticker);
        } catch (Exception e) {
            log.warn("Dividend radar failed to fetch {}, skipping: {}", ticker, e.getMessage());
            return List.of();
        }
    }

    /** Caps each ticker's contribution to the (already most-recent-first) history list. */
    private List<DividendRadarEntryDTO> boundPerTicker(List<DividendRadarEntryDTO> sortedHistory, int maxPerTicker) {
        Map<String, Integer> countByTicker = new HashMap<>();
        return sortedHistory.stream()
                .filter(entry -> countByTicker.merge(entry.ticker(), 1, Integer::sum) <= maxPerTicker)
                .collect(Collectors.toList());
    }
}
