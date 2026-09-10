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
public class CreateHealthTransactionUseCaseImpl implements CreateHealthTransactionUseCase {

    private final HealthStore store;
    private final HealthLookups lookups;

    public CreateHealthTransactionUseCaseImpl(HealthStore store, HealthLookups lookups) {
        this.store = store;
        this.lookups = lookups;
    }

    @Override
    @Transactional
    public Transaction execute(String email, TransactionInput input) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        Account account = lookups.requireActiveAccount(userId, input.accountId());
        CurrencyCode currency = lookups.requireCurrency(profile, input.currency());
        lookups.requireSameCurrency(account.currency(), currency);
        EntryType type = publicEntryType(input.type());
        EntryStatus status = enumValue(EntryStatus.class, input.status(), "status");
        BigDecimal amount = parseMoney(input.amount(), true, "amount");
        String description = requireText(input.description(), "description", 200);
        String category = optionalText(input.category(), "category", 80);
        LocalDate date = requireDate(input.date(), "date");
        String key = requireKey(input.idempotencyKey());

        Optional<Transaction> existing = store.findTransactionByIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            if (!sameTransaction(existing.get(), account.id(), type, status, amount, currency,
                    description, category, date)) {
                throw lookups.idempotencyConflict();
            }
            return existing.get();
        }
        return store.createTransaction(userId, account.id(), type, status, amount, currency,
                description, category, date, RecordSource.MANUAL, null, null, key,
                null, null, null, null);
    }
}
