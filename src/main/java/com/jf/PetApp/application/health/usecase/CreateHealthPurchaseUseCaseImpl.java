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
public class CreateHealthPurchaseUseCaseImpl implements CreateHealthPurchaseUseCase {

    private final HealthStore store;
    private final HealthLookups lookups;
    private final InvoiceCycleResolver invoices;

    public CreateHealthPurchaseUseCaseImpl(HealthStore store, HealthLookups lookups, InvoiceCycleResolver invoices) {
        this.store = store;
        this.lookups = lookups;
        this.invoices = invoices;
    }

    @Override
    @Transactional
    public PurchaseWithInstallments execute(String email, long cardId, PurchaseInput input) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        Card card = lookups.requireActiveCard(userId, cardId);
        CurrencyCode currency = lookups.requireCurrency(profile, input.currency());
        lookups.requireSameCurrency(card.currency(), currency);
        BigDecimal total = parseMoney(input.amount(), true, "amount");
        String description = requireText(input.description(), "description", 200);
        String category = optionalText(input.category(), "category", 80);
        LocalDate purchaseDate = requireDate(input.purchaseDate(), "purchaseDate");
        if (input.installmentCount() < 1 || input.installmentCount() > 120) {
            throw new IllegalArgumentException("installmentCount must be between 1 and 120");
        }
        String key = requireKey(input.idempotencyKey());
        Optional<Purchase> existing = store.findPurchaseByIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            Purchase p = existing.get();
            if (p.cardId() != cardId || p.totalAmount().compareTo(total) != 0 || p.currency() != currency
                    || !p.description().equals(description) || !Objects.equals(p.category(), category)
                    || !p.purchaseDate().equals(purchaseDate) || p.installmentCount() != input.installmentCount()) {
                throw lookups.idempotencyConflict();
            }
            return new PurchaseWithInstallments(p, store.listInstallmentsByPurchase(userId, p.id()));
        }

        Purchase purchase = store.createPurchase(userId, cardId, total, currency, description, category,
                purchaseDate, input.installmentCount(), RecordSource.MANUAL, null, null, key);
        List<BigDecimal> split = splitInstallments(total, input.installmentCount());
        YearMonth firstCycle = firstInvoiceCycle(card, purchaseDate);
        for (int index = 0; index < split.size(); index++) {
            YearMonth cycle = firstCycle.plusMonths(index);
            Invoice invoice = invoices.getOrCreateInvoice(userId, card, cycle);
            store.createInstallment(userId, purchase.id(), invoice.id(), currency,
                    index + 1, split.size(), split.get(index));
        }
        return new PurchaseWithInstallments(purchase, store.listInstallmentsByPurchase(userId, purchase.id()));
    }
}
