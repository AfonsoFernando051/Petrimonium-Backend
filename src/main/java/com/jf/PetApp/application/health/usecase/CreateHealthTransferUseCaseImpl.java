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
public class CreateHealthTransferUseCaseImpl implements CreateHealthTransferUseCase {

    private final HealthStore store;
    private final HealthLookups lookups;

    public CreateHealthTransferUseCaseImpl(HealthStore store, HealthLookups lookups) {
        this.store = store;
        this.lookups = lookups;
    }

    @Override
    @Transactional
    public TransferView execute(String email, TransferInput input) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        Account from = lookups.requireActiveAccount(userId, input.fromAccountId());
        Account to = lookups.requireActiveAccount(userId, input.toAccountId());
        if (from.id() == to.id()) {
            throw new IllegalArgumentException("fromAccountId and toAccountId must be different");
        }
        CurrencyCode currency = lookups.requireCurrency(profile, input.currency());
        lookups.requireSameCurrency(from.currency(), currency);
        lookups.requireSameCurrency(to.currency(), currency);
        BigDecimal amount = parseMoney(input.amount(), true, "amount");
        LocalDate date = requireDate(input.date(), "date");
        String description = optionalText(input.description(), "description", 200);
        String key = requireKey(input.idempotencyKey());

        Optional<Transfer> existing = store.findTransferByIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            Transfer transfer = existing.get();
            if (transfer.fromAccountId() != from.id() || transfer.toAccountId() != to.id()
                    || transfer.amount().compareTo(amount) != 0 || transfer.currency() != currency
                    || !transfer.date().equals(date) || !Objects.equals(transfer.description(), description)) {
                throw lookups.idempotencyConflict();
            }
            return transferView(userId, transfer);
        }

        Transfer transfer = store.createTransfer(userId, from.id(), to.id(), amount, currency,
                date, description, key);
        String label = description == null ? "Transferência entre contas" : description;
        store.createTransaction(userId, from.id(), EntryType.TRANSFER_OUT, EntryStatus.REALIZED,
                amount, currency, label, null, date, RecordSource.SYSTEM, null, null,
                "transfer:" + transfer.id() + ":out", transfer.id(), null, null, null);
        store.createTransaction(userId, to.id(), EntryType.TRANSFER_IN, EntryStatus.REALIZED,
                amount, currency, label, null, date, RecordSource.SYSTEM, null, null,
                "transfer:" + transfer.id() + ":in", transfer.id(), null, null, null);
        return transferView(userId, transfer);
    }

    private TransferView transferView(long userId, Transfer transfer) {
        List<Transaction> legs = store.findTransactionsByTransfer(userId, transfer.id());
        Transaction out = legs.stream().filter(t -> t.type() == EntryType.TRANSFER_OUT).findFirst()
                .orElseThrow(() -> new IllegalStateException("Transfer OUT leg missing"));
        Transaction in = legs.stream().filter(t -> t.type() == EntryType.TRANSFER_IN).findFirst()
                .orElseThrow(() -> new IllegalStateException("Transfer IN leg missing"));
        return new TransferView(transfer, out, in);
    }
}
