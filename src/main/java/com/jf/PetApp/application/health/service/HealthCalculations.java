package com.jf.PetApp.application.health.service;

import static com.jf.PetApp.core.domain.health.HealthModels.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * The pure arithmetic behind Health's card and recurrence rules: how a total
 * splits into installments, which invoice cycle a purchase lands in, and how
 * a day-of-month is clamped to months that are too short.
 *
 * No framework dependencies and no store access, in the style of
 * {@code LevelCalculator} and {@code StreakCalculator} — every value is
 * recomputed from its arguments, which is what lets
 * {@code HealthCalculationRulesTest} assert on them directly.
 */
public final class HealthCalculations {

    private HealthCalculations() {
    }

    public static YearMonth firstInvoiceCycle(Card card, LocalDate purchaseDate) {
        YearMonth purchaseMonth = YearMonth.from(purchaseDate);
        LocalDate closing = clampedDate(purchaseMonth, card.closingDay());
        return purchaseDate.isAfter(closing) ? purchaseMonth.plusMonths(1) : purchaseMonth;
    }

    public static LocalDate clampedDate(YearMonth month, int requestedDay) {
        return month.atDay(Math.min(requestedDay, month.lengthOfMonth()));
    }

    public static List<BigDecimal> splitInstallments(BigDecimal total, int count) {
        if (total == null || total.scale() > 2 || total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("total must be a positive scale-2 monetary value");
        }
        if (count < 1 || count > 120) {
            throw new IllegalArgumentException("installmentCount must be between 1 and 120");
        }
        BigInteger minor = total.movePointRight(2).toBigIntegerExact();
        BigInteger[] division = minor.divideAndRemainder(BigInteger.valueOf(count));
        List<BigDecimal> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BigInteger cents = division[0].add(i < division[1].intValueExact() ? BigInteger.ONE : BigInteger.ZERO);
            values.add(new BigDecimal(cents, 2));
        }
        return List.copyOf(values);
    }
}
