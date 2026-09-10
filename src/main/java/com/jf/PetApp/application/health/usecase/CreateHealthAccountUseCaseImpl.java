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
public class CreateHealthAccountUseCaseImpl implements CreateHealthAccountUseCase {

    private final HealthStore store;
    private final HealthLookups lookups;

    public CreateHealthAccountUseCaseImpl(HealthStore store, HealthLookups lookups) {
        this.store = store;
        this.lookups = lookups;
    }

    @Override
    @Transactional
    public AccountView execute(String email, AccountInput input) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        CurrencyCode currency = lookups.requireCurrency(profile, input.currency());
        String key = requireKey(input.idempotencyKey());
        BigDecimal initialBalance = parseMoney(input.initialBalance(), false, "initialBalance");
        String name = requireText(input.name(), "name", 100);
        AccountType type = enumValue(AccountType.class, input.type(), "type");
        LocalDate referenceDate = requireDate(input.balanceReferenceDate(), "balanceReferenceDate");

        Optional<Account> existing = store.findAccountByIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            Account account = existing.get();
            if (!sameAccount(account, name, type, initialBalance, referenceDate, currency)) {
                throw lookups.idempotencyConflict();
            }
            return new AccountView(account, money(store.accountBalance(userId, account)));
        }
        Account account = store.createAccount(userId, name, type, initialBalance, referenceDate, currency, key);
        return new AccountView(account, money(store.accountBalance(userId, account)));
    }
}
