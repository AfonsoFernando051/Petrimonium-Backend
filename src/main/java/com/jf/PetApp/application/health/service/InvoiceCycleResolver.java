package com.jf.PetApp.application.health.service;

import static com.jf.PetApp.core.domain.health.HealthModels.*;
import static com.jf.PetApp.application.health.service.HealthCalculations.clampedDate;

import com.jf.PetApp.application.health.port.HealthStore;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Finds, or opens, the invoice a card purchase belongs to for a given cycle.
 *
 * The due date is derived from the card's closing and due days, both clamped
 * to months that do not have them — a card that closes on the 30th still
 * closes in February.
 */
@Component
public class InvoiceCycleResolver {

    private final HealthStore store;

    public InvoiceCycleResolver(HealthStore store) {
        this.store = store;
    }

    public Invoice getOrCreateInvoice(long userId, Card card, YearMonth cycle) {
        return store.findInvoiceByCardAndCycle(userId, card.id(), cycle)
                .orElseGet(() -> {
                    LocalDate closing = clampedDate(cycle, card.closingDay());
                    YearMonth dueMonth = cycle;
                    LocalDate due = clampedDate(dueMonth, card.dueDay());
                    if (!due.isAfter(closing)) {
                        dueMonth = dueMonth.plusMonths(1);
                        due = clampedDate(dueMonth, card.dueDay());
                    }
                    return store.createInvoice(userId, card.id(), card.currency(), cycle, closing, due);
                });
    }
}
