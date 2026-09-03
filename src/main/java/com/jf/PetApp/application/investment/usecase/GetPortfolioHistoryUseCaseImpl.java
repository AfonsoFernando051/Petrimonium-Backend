package com.jf.PetApp.application.investment.usecase;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.jf.PetApp.application.investment.dto.InvestmentLotDTO;
import com.jf.PetApp.application.investment.dto.PortfolioHistoryPointDTO;

// NOTE: This is a deterministic cost-basis -> current-value interpolation approximation, not true
// historical mark-to-market data, since no daily price history is persisted anywhere in this system.
@Service
public class GetPortfolioHistoryUseCaseImpl implements GetPortfolioHistoryUseCase {

    private static final int SAMPLE_COUNT = 60;

    private final GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase;

    public GetPortfolioHistoryUseCaseImpl(GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase) {
        this.getPortfolioHoldingsUseCase = getPortfolioHoldingsUseCase;
    }

    @Override
    public List<PortfolioHistoryPointDTO> execute(String email, String range) {
        List<InvestmentLotDTO> lots = getPortfolioHoldingsUseCase.execute(email);
        LocalDate today = LocalDate.now();

        LocalDate windowStart = resolveWindowStart(range, today, lots);
        List<LocalDate> sampleDates = generateSampleDates(windowStart, today);

        List<PortfolioHistoryPointDTO> points = new ArrayList<>();
        for (LocalDate d : sampleDates) {
            double investedCapital = 0.0;
            double portfolioValue = 0.0;

            for (InvestmentLotDTO lot : lots) {
                if (lot.purchaseDate().isAfter(d)) {
                    continue;
                }

                double investedContribution = lot.quantity() * lot.purchasePrice();

                double progress;
                if (today.equals(lot.purchaseDate())) {
                    progress = 1.0;
                } else {
                    long totalDays = ChronoUnit.DAYS.between(lot.purchaseDate(), today);
                    long elapsedDays = ChronoUnit.DAYS.between(lot.purchaseDate(), d);
                    progress = totalDays == 0 ? 1.0 : clamp((double) elapsedDays / (double) totalDays, 0.0, 1.0);
                }

                double valueContribution = lot.quantity() * (lot.purchasePrice() + (lot.currentPrice() - lot.purchasePrice()) * progress);

                investedCapital += investedContribution;
                portfolioValue += valueContribution;
            }

            points.add(new PortfolioHistoryPointDTO(d, investedCapital, portfolioValue));
        }

        return points;
    }

    private LocalDate resolveWindowStart(String range, LocalDate today, List<InvestmentLotDTO> lots) {
        String normalized = range == null ? "ALL" : range.trim().toUpperCase();

        return switch (normalized) {
            case "7D" -> today.minusDays(7);
            case "30D" -> today.minusDays(30);
            case "3M" -> today.minusMonths(3);
            case "6M" -> today.minusMonths(6);
            case "1Y" -> today.minusYears(1);
            case "3Y" -> today.minusYears(3);
            case "5Y" -> today.minusYears(5);
            default -> lots.stream()
                    .map(InvestmentLotDTO::purchaseDate)
                    .min(LocalDate::compareTo)
                    .orElse(today);
        };
    }

    private List<LocalDate> generateSampleDates(LocalDate windowStart, LocalDate today) {
        if (windowStart.isAfter(today)) {
            windowStart = today;
        }

        long totalDays = ChronoUnit.DAYS.between(windowStart, today);

        Set<LocalDate> dates = new LinkedHashSet<>();
        if (totalDays <= 0) {
            dates.add(today);
            return new ArrayList<>(dates);
        }

        if (totalDays < SAMPLE_COUNT) {
            for (long i = 0; i <= totalDays; i++) {
                dates.add(windowStart.plusDays(i));
            }
            return new ArrayList<>(dates);
        }

        int samples = SAMPLE_COUNT;
        for (int i = 0; i < samples; i++) {
            double fraction = (double) i / (double) (samples - 1);
            long offsetDays = Math.round(fraction * totalDays);
            dates.add(windowStart.plusDays(offsetDays));
        }
        // Ensure "today" is always included as the final sample.
        dates.add(today);

        return new ArrayList<>(dates);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
