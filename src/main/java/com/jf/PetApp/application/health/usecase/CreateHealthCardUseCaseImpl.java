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
public class CreateHealthCardUseCaseImpl implements CreateHealthCardUseCase {

    private final HealthStore store;
    private final HealthLookups lookups;

    public CreateHealthCardUseCaseImpl(HealthStore store, HealthLookups lookups) {
        this.store = store;
        this.lookups = lookups;
    }

    @Override
    @Transactional
    public Card execute(String email, CardInput input) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        CurrencyCode currency = lookups.requireCurrency(profile, input.currency());
        String name = requireText(input.name(), "name", 100);
        int closingDay = requireDay(input.closingDay(), "closingDay");
        int dueDay = requireDay(input.dueDay(), "dueDay");
        String key = requireKey(input.idempotencyKey());
        Optional<Card> existing = store.findCardByIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            Card card = existing.get();
            if (!card.name().equals(name) || card.currency() != currency
                    || card.closingDay() != closingDay || card.dueDay() != dueDay) {
                throw lookups.idempotencyConflict();
            }
            return card;
        }
        return store.createCard(userId, name, currency, closingDay, dueDay, key);
    }
}
