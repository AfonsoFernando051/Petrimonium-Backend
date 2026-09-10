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
public class CreateHealthRecurrenceUseCaseImpl implements CreateHealthRecurrenceUseCase {

    private final HealthStore store;
    private final HealthLookups lookups;
    private final RecurrenceMaterializer recurrences;

    public CreateHealthRecurrenceUseCaseImpl(HealthStore store, HealthLookups lookups, RecurrenceMaterializer recurrences) {
        this.store = store;
        this.lookups = lookups;
        this.recurrences = recurrences;
    }

    @Override
    @Transactional
    public Recurrence execute(String email, RecurrenceInput input) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
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
        String key = requireKey(input.idempotencyKey());
        String description = requireText(input.description(), "description", 200);
        String category = optionalText(input.category(), "category", 80);

        Optional<Recurrence> existing = store.findRecurrenceByIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            Recurrence r = existing.get();
            if (!sameRecurrence(r, account.id(), type, amount, currency, description, category,
                    day, start, input.endDate())) {
                throw lookups.idempotencyConflict();
            }
            return r;
        }
        Recurrence recurrence = store.createRecurrence(userId, account.id(), type, amount, currency,
                description, category, day, start, input.endDate(), key);
        recurrences.materializeRecurrence(userId, recurrence, YearMonth.now());
        return recurrence;
    }
}
