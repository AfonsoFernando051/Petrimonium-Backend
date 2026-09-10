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
public class PayHealthInvoiceUseCaseImpl implements PayHealthInvoiceUseCase {

    private final HealthStore store;
    private final HealthLookups lookups;

    public PayHealthInvoiceUseCaseImpl(HealthStore store, HealthLookups lookups) {
        this.store = store;
        this.lookups = lookups;
    }

    @Override
    @Transactional
    public InvoiceWithTotal execute(String email, long invoiceId, InvoicePaymentInput input) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        Invoice invoice = lookups.requireInvoice(userId, invoiceId);
        Account account = lookups.requireActiveAccount(userId, input.accountId());
        CurrencyCode currency = lookups.requireCurrency(profile, input.currency());
        lookups.requireSameCurrency(invoice.currency(), currency);
        lookups.requireSameCurrency(account.currency(), currency);
        LocalDate paymentDate = requireDate(input.paymentDate(), "paymentDate");
        String key = requireKey(input.idempotencyKey());

        Optional<Transaction> existingPayment = store.findTransactionByIdempotencyKey(userId, key);
        if (existingPayment.isPresent()) {
            if (!Objects.equals(existingPayment.get().invoiceId(), invoiceId)
                    || existingPayment.get().accountId() != account.id()) {
                throw lookups.idempotencyConflict();
            }
            Invoice current = lookups.requireInvoice(userId, invoiceId);
            return new InvoiceWithTotal(current, money(store.invoiceTotal(userId, invoiceId)));
        }
        if (invoice.status() == InvoiceStatus.PAID) {
            throw new HealthConflictException("INVOICE_ALREADY_PAID", "Esta fatura já foi paga.");
        }
        BigDecimal total = money(store.invoiceTotal(userId, invoiceId));
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new HealthConflictException("EMPTY_INVOICE", "Uma fatura sem prestações não pode ser paga.");
        }
        Transaction payment = store.createTransaction(userId, account.id(), EntryType.INVOICE_PAYMENT,
                EntryStatus.REALIZED, total, currency, "Pagamento de fatura " + invoice.cycleMonth(),
                null, paymentDate, RecordSource.SYSTEM, null, null, key,
                null, null, null, invoice.id());
        Invoice paid = store.markInvoicePaid(userId, invoice.id(), paymentDate, payment.id());
        return new InvoiceWithTotal(paid, total);
    }
}
