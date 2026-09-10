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
public class SaveHealthProfileUseCaseImpl implements SaveHealthProfileUseCase {

    /**
     * Refusing a currency change once money exists is a data-integrity rule,
     * not a validation message: the stored amounts carry no exchange rate, so
     * reinterpreting them in another currency would silently restate the
     * user's balances.
     */
    private static final String CURRENCY_LOCKED_MESSAGE =
            "A moeda principal não pode ser alterada porque já existem dados financeiros. "
                    + "Uma migração de moeda será necessária para preservar os valores existentes.";

    private final HealthStore store;
    private final HealthLookups lookups;

    public SaveHealthProfileUseCaseImpl(HealthStore store, HealthLookups lookups) {
        this.store = store;
        this.lookups = lookups;
    }

    @Override
    @Transactional
    public ProfileView execute(String email, ProfileInput input) {
        long userId = lookups.userId(email);
        CountryCode country = enumValue(CountryCode.class, input.countryCode(), "countryCode");
        CurrencyCode currency = enumValue(CurrencyCode.class, input.primaryCurrency(), "primaryCurrency");
        String localeTag = requireLocale(input.localeTag());

        Optional<Profile> current = store.findProfileForUpdate(userId);
        Profile saved;
        if (current.isEmpty()) {
            saved = store.createProfile(userId, country, currency, localeTag);
        } else {
            boolean hasData = store.hasFinancialData(userId);
            if (hasData && current.get().primaryCurrency() != currency) {
                throw new HealthConflictException("CURRENCY_CHANGE_LOCKED", CURRENCY_LOCKED_MESSAGE);
            }
            saved = store.updateProfile(userId, country, currency, localeTag);
        }
        return new ProfileView(saved, !store.hasFinancialData(userId));
    }
}
