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
public class GetHealthSummaryUseCaseImpl implements GetHealthSummaryUseCase {

    private final HealthStore store;
    private final HealthLookups lookups;
    private final RecurrenceMaterializer recurrences;

    public GetHealthSummaryUseCaseImpl(HealthStore store, HealthLookups lookups, RecurrenceMaterializer recurrences) {
        this.store = store;
        this.lookups = lookups;
        this.recurrences = recurrences;
    }

    @Override
    @Transactional
    public MonthlySummary execute(String email, YearMonth month) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        YearMonth requestedMonth = month == null ? YearMonth.now() : month;
        recurrences.materializeRecurrences(userId, requestedMonth);

        BigDecimal currentBalance = store.listAccounts(userId).stream()
                .filter(a -> !a.archived())
                .map(a -> store.accountBalance(userId, a))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Transaction> transactions = store.listTransactions(userId).stream()
                .filter(t -> YearMonth.from(t.date()).equals(requestedMonth))
                .toList();
        BigDecimal realizedIncome = sumTransactions(transactions, EntryType.INCOME, EntryStatus.REALIZED);
        BigDecimal directRealizedExpenses = sumTransactions(transactions, EntryType.EXPENSE, EntryStatus.REALIZED);
        BigDecimal plannedIncome = sumTransactions(transactions, EntryType.INCOME, EntryStatus.PLANNED);
        BigDecimal plannedExpenses = sumTransactions(transactions, EntryType.EXPENSE, EntryStatus.PLANNED);

        List<Invoice> invoices = store.listInvoices(userId);
        List<Invoice> cycleInvoices = invoices.stream()
                .filter(i -> i.cycleMonth().equals(requestedMonth)).toList();
        BigDecimal cardExpenses = cycleInvoices.stream()
                .map(i -> store.invoiceTotal(userId, i.id())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal realizedExpenses = directRealizedExpenses.add(cardExpenses);

        List<Invoice> openDueInvoices = invoices.stream()
                .filter(i -> i.status() == InvoiceStatus.OPEN)
                .filter(i -> YearMonth.from(i.dueDate()).equals(requestedMonth))
                .toList();
        BigDecimal openInvoices = openDueInvoices.stream()
                .map(i -> store.invoiceTotal(userId, i.id())).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, Purchase> purchases = new LinkedHashMap<>();
        for (Purchase purchase : store.listPurchases(userId)) {
            purchases.put(purchase.id(), purchase);
        }
        Map<String, BigDecimal> categories = new LinkedHashMap<>();
        transactions.stream()
                .filter(t -> t.type() == EntryType.EXPENSE && t.status() == EntryStatus.REALIZED)
                .forEach(t -> categories.merge(categoryOrOther(t.category()), t.amount(), BigDecimal::add));
        for (Invoice invoice : cycleInvoices) {
            for (Installment installment : store.listInstallmentsByInvoice(userId, invoice.id())) {
                Purchase purchase = purchases.get(installment.purchaseId());
                categories.merge(categoryOrOther(purchase == null ? null : purchase.category()),
                        installment.amount(), BigDecimal::add);
            }
        }

        List<Upcoming> upcoming = new ArrayList<>();
        transactions.stream().filter(t -> t.status() == EntryStatus.PLANNED)
                .forEach(t -> upcoming.add(new Upcoming("TRANSACTION", t.id(), t.description(),
                        t.date(), t.amount())));
        for (Invoice invoice : openDueInvoices) {
            upcoming.add(new Upcoming("CARD_INVOICE", invoice.id(), "Fatura do cartão",
                    invoice.dueDate(), store.invoiceTotal(userId, invoice.id())));
        }
        upcoming.sort(Comparator.comparing(Upcoming::date).thenComparing(Upcoming::kind));

        BigDecimal monthResult = realizedIncome.subtract(realizedExpenses);
        BigDecimal projected = currentBalance.add(plannedIncome).subtract(plannedExpenses).subtract(openInvoices);
        List<CategoryAmount> categoryAmounts = categories.entrySet().stream()
                .map(e -> new CategoryAmount(e.getKey(), money(e.getValue())))
                .sorted(Comparator.comparing(CategoryAmount::amount).reversed())
                .toList();

        return new MonthlySummary(requestedMonth, profile.primaryCurrency(), money(currentBalance),
                money(realizedIncome), money(realizedExpenses), money(plannedIncome), money(plannedExpenses),
                money(openInvoices), money(monthResult), money(projected), categoryAmounts, upcoming);
    }

    private BigDecimal sumTransactions(List<Transaction> transactions, EntryType type, EntryStatus status) {
        return transactions.stream().filter(t -> t.type() == type && t.status() == status)
                .map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
