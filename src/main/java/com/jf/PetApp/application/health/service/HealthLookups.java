package com.jf.PetApp.application.health.service;

import static com.jf.PetApp.core.domain.health.HealthModels.*;
import static com.jf.PetApp.application.health.service.HealthValidation.enumValue;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.health.exception.HealthConflictException;
import com.jf.PetApp.application.health.port.HealthStore;
import com.jf.PetApp.application.user.port.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Resolves the entities a Health request names, failing with the right error
 * when one is missing, archived or in the wrong currency.
 *
 * Every Health use case starts by turning an email into a user id and
 * checking that what the caller referenced actually belongs to them; keeping
 * that in one collaborator is what stops each use case from re-deciding what
 * "not found" and "archived" mean.
 */
@Component
public class HealthLookups {

    private final HealthStore store;
    private final UserRepository userRepository;

    public HealthLookups(HealthStore store, UserRepository userRepository) {
        this.store = store;
        this.userRepository = userRepository;
    }

    public long userId(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getId() != null)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }

    public Profile requireProfile(long userId) {
        return store.findProfile(userId).orElseThrow(() ->
                new ResourceNotFoundException("Health profile not found. Complete Health onboarding first."));
    }

    public Account requireAccount(long userId, long accountId) {
        return store.findAccount(userId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    public Account requireActiveAccount(long userId, long accountId) {
        Account account = requireAccount(userId, accountId);
        if (account.archived()) {
            throw new HealthConflictException("ACCOUNT_ARCHIVED", "A conta está arquivada.");
        }
        return account;
    }

    public Transaction requireTransaction(long userId, long transactionId) {
        return store.findTransaction(userId, transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    public Recurrence requireRecurrence(long userId, long recurrenceId) {
        return store.findRecurrence(userId, recurrenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurrence not found"));
    }

    public Card requireCard(long userId, long cardId) {
        return store.findCard(userId, cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    }

    public Card requireActiveCard(long userId, long cardId) {
        Card card = requireCard(userId, cardId);
        if (card.archived()) {
            throw new HealthConflictException("CARD_ARCHIVED", "O cartão está arquivado.");
        }
        return card;
    }

    public Invoice requireInvoice(long userId, long invoiceId) {
        return store.findInvoice(userId, invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
    }

    public CurrencyCode requireCurrency(Profile profile, String requested) {
        CurrencyCode currency = enumValue(CurrencyCode.class, requested, "currency");
        if (profile.primaryCurrency() != currency) {
            throw currencyMismatch(profile.primaryCurrency(), currency);
        }
        return currency;
    }

    public void requireSameCurrency(CurrencyCode expected, CurrencyCode actual) {
        if (expected != actual) {
            throw currencyMismatch(expected, actual);
        }
    }

    public HealthConflictException currencyMismatch(CurrencyCode expected, CurrencyCode actual) {
        return new HealthConflictException("CURRENCY_MISMATCH",
                "A moeda do registro (" + actual + ") não corresponde à moeda principal (" + expected + ").");
    }

    public HealthConflictException idempotencyConflict() {
        return new HealthConflictException("IDEMPOTENCY_KEY_REUSED",
                "A chave de idempotência já foi usada com dados diferentes.");
    }
}
