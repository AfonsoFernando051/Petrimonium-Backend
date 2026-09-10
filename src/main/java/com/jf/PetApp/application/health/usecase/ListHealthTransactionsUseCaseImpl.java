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
public class ListHealthTransactionsUseCaseImpl implements ListHealthTransactionsUseCase {

    private final HealthStore store;
    private final HealthLookups lookups;
    private final RecurrenceMaterializer recurrences;

    public ListHealthTransactionsUseCaseImpl(HealthStore store, HealthLookups lookups, RecurrenceMaterializer recurrences) {
        this.store = store;
        this.lookups = lookups;
        this.recurrences = recurrences;
    }

    @Override
    @Transactional
    public List<Transaction> execute(String email, TransactionFilter filter) {
        long userId = lookups.userId(email);
        lookups.requireProfile(userId);
        if (filter != null && filter.to() != null) {
            recurrences.materializeRecurrences(userId, YearMonth.from(filter.to()));
        } else {
            recurrences.materializeRecurrences(userId, YearMonth.now());
        }
        EntryStatus wantedStatus = filter == null || filter.status() == null || filter.status().isBlank()
                ? null : enumValue(EntryStatus.class, filter.status(), "status");
        return store.listTransactions(userId).stream()
                .filter(tx -> filter == null || filter.from() == null || !tx.date().isBefore(filter.from()))
                .filter(tx -> filter == null || filter.to() == null || !tx.date().isAfter(filter.to()))
                .filter(tx -> filter == null || filter.accountId() == null || tx.accountId() == filter.accountId())
                .filter(tx -> filter == null || filter.category() == null || filter.category().isBlank()
                        || Objects.equals(normalizeCategory(tx.category()), normalizeCategory(filter.category())))
                .filter(tx -> wantedStatus == null || tx.status() == wantedStatus)
                .toList();
    }
}
