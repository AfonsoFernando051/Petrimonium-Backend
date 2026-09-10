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
public class UpdateHealthAccountUseCaseImpl implements UpdateHealthAccountUseCase {

    private final HealthStore store;
    private final HealthLookups lookups;

    public UpdateHealthAccountUseCaseImpl(HealthStore store, HealthLookups lookups) {
        this.store = store;
        this.lookups = lookups;
    }

    @Override
    @Transactional
    public AccountView execute(String email, long accountId, AccountInput input) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        Account current = lookups.requireAccount(userId, accountId);
        CurrencyCode currency = lookups.requireCurrency(profile, input.currency());
        if (current.currency() != currency) {
            throw lookups.currencyMismatch(profile.primaryCurrency(), currency);
        }
        Account updated = store.updateAccount(userId, accountId,
                requireText(input.name(), "name", 100), enumValue(AccountType.class, input.type(), "type"),
                parseMoney(input.initialBalance(), false, "initialBalance"),
                requireDate(input.balanceReferenceDate(), "balanceReferenceDate"), currency);
        return new AccountView(updated, money(store.accountBalance(userId, updated)));
    }
}
