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
public class UpdateHealthTransactionUseCaseImpl implements UpdateHealthTransactionUseCase {

    private final HealthStore store;
    private final HealthLookups lookups;

    public UpdateHealthTransactionUseCaseImpl(HealthStore store, HealthLookups lookups) {
        this.store = store;
        this.lookups = lookups;
    }

    @Override
    @Transactional
    public Transaction execute(String email, long transactionId, TransactionInput input) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        Transaction current = lookups.requireTransaction(userId, transactionId);
        if (current.type() == EntryType.TRANSFER_IN || current.type() == EntryType.TRANSFER_OUT
                || current.type() == EntryType.INVOICE_PAYMENT) {
            throw new HealthConflictException("SYSTEM_ENTRY_IMMUTABLE",
                    "Transferências e pagamentos de fatura devem ser alterados pelo fluxo que os criou.");
        }
        Account account = lookups.requireActiveAccount(userId, input.accountId());
        CurrencyCode currency = lookups.requireCurrency(profile, input.currency());
        lookups.requireSameCurrency(account.currency(), currency);
        return store.updateTransaction(userId, transactionId, account.id(), publicEntryType(input.type()),
                enumValue(EntryStatus.class, input.status(), "status"),
                parseMoney(input.amount(), true, "amount"), currency,
                requireText(input.description(), "description", 200),
                optionalText(input.category(), "category", 80), requireDate(input.date(), "date"));
    }
}
