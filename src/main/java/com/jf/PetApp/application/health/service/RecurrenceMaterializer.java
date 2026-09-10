package com.jf.PetApp.application.health.service;

import static com.jf.PetApp.core.domain.health.HealthModels.*;
import static com.jf.PetApp.application.health.service.HealthCalculations.clampedDate;

import com.jf.PetApp.application.health.port.HealthStore;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Turns an active recurrence into the planned transactions it implies, up to
 * a given month.
 *
 * Generation is idempotent per (recurrence, month) through a derived
 * idempotency key, so asking for the same month twice never doubles a
 * commitment — the property `generatingTheSameMonthAgainDoesNotDuplicateThe`
 * `Commitment` pins. Reading a month is what materializes it, which is why
 * listing transactions and building the monthly summary both come through
 * here.
 */
@Component
public class RecurrenceMaterializer {

    /** Guards against a start date far in the past generating unbounded rows. */
    private static final int MAX_RECURRENCE_MONTHS_PER_CALL = 240;

    private final HealthStore store;

    public RecurrenceMaterializer(HealthStore store) {
        this.store = store;
    }

    public void materializeRecurrences(long userId, YearMonth through) {
        for (Recurrence recurrence : store.listRecurrences(userId)) {
            if (recurrence.active()) {
                materializeRecurrence(userId, recurrence, through);
            }
        }
    }

    public void materializeRecurrence(long userId, Recurrence recurrence, YearMonth through) {
        YearMonth cursor = YearMonth.from(recurrence.startDate());
        int generatedOrVisited = 0;
        while (!cursor.isAfter(through)) {
            if (++generatedOrVisited > MAX_RECURRENCE_MONTHS_PER_CALL) {
                throw new IllegalArgumentException("Recurrence range exceeds 240 months");
            }
            if (monthIsWithin(recurrence, cursor)) {
                String key = "recurrence:" + recurrence.id() + ":" + cursor;
                if (store.findTransactionByIdempotencyKey(userId, key).isEmpty()) {
                    LocalDate due = clampedDate(cursor, recurrence.dayOfMonth());
                    store.createTransaction(userId, recurrence.accountId(), recurrence.type(), EntryStatus.PLANNED,
                            recurrence.amount(), recurrence.currency(), recurrence.description(), recurrence.category(),
                            due, RecordSource.SYSTEM, null, null, key, null, recurrence.id(), cursor, null);
                }
            }
            cursor = cursor.plusMonths(1);
        }
    }

    public boolean monthIsWithin(Recurrence recurrence, YearMonth month) {
        LocalDate due = clampedDate(month, recurrence.dayOfMonth());
        return !due.isBefore(recurrence.startDate())
                && (recurrence.endDate() == null || !due.isAfter(recurrence.endDate()));
    }
}
