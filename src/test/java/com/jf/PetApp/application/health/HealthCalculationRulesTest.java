package com.jf.PetApp.application.health;

import static com.jf.PetApp.core.domain.health.HealthModels.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * The arithmetic the Health app's numbers rest on, tested without a database: installment
 * splitting, the invoice a purchase lands on, and the due date a monthly commitment gets in a
 * month that has no such day. {@code HealthServiceIntegrationTest} covers the same rules again
 * through real SQL; these are here so a broken cent is reported as one failing assertion rather
 * than as a wrong balance three layers up.
 */
class HealthCalculationRulesTest {

    private static Card card(int closingDay, int dueDay) {
        return new Card(1L, 1L, "Cartão", CurrencyCode.BRL, closingDay, dueDay, false, "k",
                Instant.EPOCH, Instant.EPOCH);
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void installmentsOfAnExactlyDivisibleTotalAreAllEqual() {
        List<BigDecimal> parts = HealthService.splitInstallments(new BigDecimal("999.99"), 3);

        assertEquals(List.of(new BigDecimal("333.33"), new BigDecimal("333.33"), new BigDecimal("333.33")), parts);
        assertEquals(0, sum(parts).compareTo(new BigDecimal("999.99")));
    }

    @Test
    void anIndivisibleTotalPutsTheLeftoverCentsOnTheEarliestInstallments() {
        // 100.00 / 3 = 33.333... The cent that cannot be split is charged first, never dropped:
        // 33.33 x 3 would silently lose a cent of the user's money.
        List<BigDecimal> parts = HealthService.splitInstallments(new BigDecimal("100.00"), 3);

        assertEquals(List.of(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33")), parts);
        assertEquals(0, sum(parts).compareTo(new BigDecimal("100.00")));
    }

    @ParameterizedTest
    @CsvSource({"0.01,1", "0.05,4", "10.00,3", "1234.56,7", "999999.99,12", "50.00,120", "0.02,3"})
    void everySplitAddsBackUpToTheOriginalTotal(String total, int count) {
        BigDecimal amount = new BigDecimal(total);
        List<BigDecimal> parts = HealthService.splitInstallments(amount, count);

        assertEquals(count, parts.size());
        assertEquals(0, sum(parts).compareTo(amount),
                "installments must sum to exactly the purchase total");
        assertTrue(parts.stream().allMatch(p -> p.scale() == 2), "every installment stays at scale 2");
    }

    @Test
    void aTotalSmallerThanOneCentPerInstallmentStillNeverInventsMoney() {
        // 0.02 over 3 installments cannot give every installment a positive value; what it must
        // never do is round each one up to a cent and charge 0.03.
        List<BigDecimal> parts = HealthService.splitInstallments(new BigDecimal("0.02"), 3);

        assertEquals(0, sum(parts).compareTo(new BigDecimal("0.02")));
    }

    @Test
    void splittingRejectsAnAmountThatIsNotMoney() {
        assertThrows(IllegalArgumentException.class,
                () -> HealthService.splitInstallments(new BigDecimal("10.005"), 2));
        assertThrows(IllegalArgumentException.class,
                () -> HealthService.splitInstallments(new BigDecimal("0.00"), 2));
        assertThrows(IllegalArgumentException.class,
                () -> HealthService.splitInstallments(new BigDecimal("-10.00"), 2));
        assertThrows(IllegalArgumentException.class,
                () -> HealthService.splitInstallments(new BigDecimal("10.00"), 0));
        assertThrows(IllegalArgumentException.class,
                () -> HealthService.splitInstallments(new BigDecimal("10.00"), 121));
    }

    @Test
    void aPurchaseBeforeTheClosingDayFallsOnTheSameMonthsInvoice() {
        assertEquals(YearMonth.of(2026, 3),
                HealthService.firstInvoiceCycle(card(20, 28), LocalDate.of(2026, 3, 19)));
    }

    @Test
    void aPurchaseOnTheClosingDayItselfStillFallsOnThatInvoice() {
        // The closing day is inclusive: the statement closes at the end of that day.
        assertEquals(YearMonth.of(2026, 3),
                HealthService.firstInvoiceCycle(card(20, 28), LocalDate.of(2026, 3, 20)));
    }

    @Test
    void aPurchaseAfterTheClosingDayRollsToTheNextInvoice() {
        assertEquals(YearMonth.of(2026, 4),
                HealthService.firstInvoiceCycle(card(20, 28), LocalDate.of(2026, 3, 21)));
    }

    @Test
    void aClosingDayThatFebruaryDoesNotHaveClosesOnItsLastDay() {
        // Closing day 31 in a 28-day February: a purchase on the 28th is still inside the cycle.
        assertEquals(YearMonth.of(2026, 2),
                HealthService.firstInvoiceCycle(card(31, 10), LocalDate.of(2026, 2, 28)));
    }

    @ParameterizedTest
    @CsvSource({
            "2026-01,31,2026-01-31",
            "2026-02,31,2026-02-28",
            "2028-02,31,2028-02-29",
            "2026-04,31,2026-04-30",
            "2026-02,30,2026-02-28",
            "2026-06,15,2026-06-15",
            "2026-06,1,2026-06-01"
    })
    void aDueDayMissingFromTheMonthMovesToItsLastDay(String month, int day, String expected) {
        assertEquals(LocalDate.parse(expected), HealthService.clampedDate(YearMonth.parse(month), day));
    }
}
