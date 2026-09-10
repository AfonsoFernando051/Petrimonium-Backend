package com.jf.PetApp.application.health.usecase;

import static com.jf.PetApp.core.domain.health.HealthModels.*;
import static com.jf.PetApp.application.health.dto.HealthCommands.*;
import static com.jf.PetApp.application.health.service.HealthCalculations.*;
import static com.jf.PetApp.application.health.service.HealthValidation.*;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.health.exception.HealthConflictException;
import com.jf.PetApp.application.health.port.HealthStore;
import com.jf.PetApp.application.health.service.HealthLookups;
import com.jf.PetApp.application.health.service.InvoiceCycleResolver;
import com.jf.PetApp.application.health.service.RecurrenceMaterializer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class UpdateHealthRecurrenceUseCaseImpl implements UpdateHealthRecurrenceUseCase {

    private final HealthStore store;
    private final HealthLookups lookups;
    private final RecurrenceMaterializer recurrences;

    public UpdateHealthRecurrenceUseCaseImpl(HealthStore store, HealthLookups lookups, RecurrenceMaterializer recurrences) {
        this.store = store;
        this.lookups = lookups;
        this.recurrences = recurrences;
    }

    @Override
    @Transactional
    public Recurrence execute(String email, long recurrenceId, RecurrenceInput input) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        lookups.requireRecurrence(userId, recurrenceId);
        Account account = lookups.requireActiveAccount(userId, input.accountId());
        CurrencyCode currency = lookups.requireCurrency(profile, input.currency());
        lookups.requireSameCurrency(account.currency(), currency);
        EntryType type = publicEntryType(input.type());
        BigDecimal amount = parseMoney(input.amount(), true, "amount");
        int day = requireDay(input.dayOfMonth(), "dayOfMonth");
        LocalDate start = requireDate(input.startDate(), "startDate");
        if (input.endDate() != null && input.endDate().isBefore(start)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        String description = requireText(input.description(), "description", 200);
        String category = optionalText(input.category(), "category", 80);
        Recurrence updated = store.updateRecurrence(userId, recurrenceId, account.id(), type, amount,
                currency, description, category, day, start, input.endDate());

        LocalDate futureBoundary = LocalDate.now().withDayOfMonth(1);
        for (Transaction occurrence : store.listTransactions(userId)) {
            if (!Objects.equals(occurrence.recurrenceId(), recurrenceId)
                    || occurrence.status() != EntryStatus.PLANNED
                    || occurrence.date().isBefore(futureBoundary)) {
                continue;
            }
            YearMonth occurrenceMonth = occurrence.recurrenceMonth();
            if (occurrenceMonth == null || !recurrences.monthIsWithin(updated, occurrenceMonth)) {
                store.softDeleteTransaction(userId, occurrence.id());
            } else {
                LocalDate due = clampedDate(occurrenceMonth, day);
                store.updateTransaction(userId, occurrence.id(), account.id(), type, EntryStatus.PLANNED,
                        amount, currency, description, category, due);
            }
        }
        recurrences.materializeRecurrence(userId, updated, YearMonth.now());
        return updated;
    }
}
