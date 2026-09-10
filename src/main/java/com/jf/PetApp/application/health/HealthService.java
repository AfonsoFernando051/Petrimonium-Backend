package com.jf.PetApp.application.health;

import static com.jf.PetApp.core.domain.health.HealthModels.*;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.health.exception.HealthConflictException;
import com.jf.PetApp.application.health.port.HealthStore;
import com.jf.PetApp.application.health.service.HealthLookups;
import com.jf.PetApp.application.user.port.UserRepository;

import static com.jf.PetApp.application.health.service.HealthCalculations.*;
import static com.jf.PetApp.application.health.service.HealthValidation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Transactional business rules for the first manual-entry Health release. */
@Service
public class HealthService {

    private static final int MAX_RECURRENCE_MONTHS_PER_CALL = 240;
    private static final String CURRENCY_LOCKED_MESSAGE =
            "A moeda principal não pode ser alterada porque já existem dados financeiros. "
                    + "Uma migração de moeda será necessária para preservar os valores existentes.";

    private final HealthStore store;
    private final HealthLookups lookups;

    public HealthService(HealthStore store, HealthLookups lookups) {
        this.store = store;
        this.lookups = lookups;
    }

    public record ProfileInput(String countryCode, String primaryCurrency, String localeTag) {}
    public record ProfileView(Profile profile, boolean currencyChangeAllowed) {}
    public record AccountInput(String name, String type, String initialBalance,
                               LocalDate balanceReferenceDate, String currency, String idempotencyKey) {}
    public record AccountView(Account account, BigDecimal currentBalance) {}
    public record TransactionInput(long accountId, String type, String status, String amount,
                                   String currency, String description, String category,
                                   LocalDate date, String idempotencyKey) {}
    public record TransactionFilter(LocalDate from, LocalDate to, Long accountId,
                                    String category, String status) {}
    public record TransferInput(long fromAccountId, long toAccountId, String amount, String currency,
                                LocalDate date, String description, String idempotencyKey) {}
    public record TransferView(Transfer transfer, Transaction outTransaction, Transaction inTransaction) {}
    public record RecurrenceInput(long accountId, String type, String amount, String currency,
                                  String description, String category, int dayOfMonth,
                                  LocalDate startDate, LocalDate endDate, String idempotencyKey) {}
    public record CardInput(String name, String currency, int closingDay, int dueDay, String idempotencyKey) {}
    public record PurchaseInput(String amount, String currency, String description, String category,
                                LocalDate purchaseDate, int installmentCount, String idempotencyKey) {}
    public record InvoicePaymentInput(long accountId, String currency, LocalDate paymentDate,
                                      String idempotencyKey) {}

    public Optional<ProfileView> getProfile(String email) {
        long userId = lookups.userId(email);
        return store.findProfile(userId).map(p -> new ProfileView(p, !store.hasFinancialData(userId)));
    }

    @Transactional
    public ProfileView saveProfile(String email, ProfileInput input) {
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

    public List<AccountView> listAccounts(String email) {
        long userId = lookups.userId(email);
        lookups.requireProfile(userId);
        return store.listAccounts(userId).stream()
                .map(a -> new AccountView(a, money(store.accountBalance(userId, a))))
                .toList();
    }

    @Transactional
    public AccountView createAccount(String email, AccountInput input) {
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

    @Transactional
    public AccountView updateAccount(String email, long accountId, AccountInput input) {
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

    @Transactional
    public void archiveAccount(String email, long accountId) {
        long userId = lookups.userId(email);
        lookups.requireAccount(userId, accountId);
        store.archiveAccount(userId, accountId);
    }

    /**
     * Transactional despite reading: {@link #materializeRecurrences} writes the month's planned
     * entries before they can be listed. Without a transaction each insert auto-commits on its
     * own, so a failure part-way through the loop leaves recurrences half-materialized, and two
     * concurrent calls both see the idempotency key as absent and race into
     * {@code uq_health_transactions_user_key} — a 500 on a read endpoint.
     */
    @Transactional
    public List<Transaction> listTransactions(String email, TransactionFilter filter) {
        long userId = lookups.userId(email);
        lookups.requireProfile(userId);
        if (filter != null && filter.to() != null) {
            materializeRecurrences(userId, YearMonth.from(filter.to()));
        } else {
            materializeRecurrences(userId, YearMonth.now());
        }
        EntryStatus wantedStatus = filter == null || filter.status() == null || filter.status().isBlank()
                ? null : enumValue(EntryStatus.class, filter.status(), "status");
        return store.listTransactions(userId).stream()
                .filter(tx -> filter == null || filter.from() == null || !tx.date().isBefore(filter.from()))
                .filter(tx -> filter == null || filter.to() == null || !tx.date().isAfter(filter.to()))
                .filter(tx -> filter == null || filter.accountId() == null || tx.accountId() == filter.accountId())
                .filter(tx -> filter == null || filter.category() == null || filter.category().isBlank()
                        || Objects.equals(normalizeCategory(tx.category()), normalizeCategory(filter.category())))
                .filter(tx -> wantedStatus == null || tx.status() == wantedStatus)
                .toList();
    }

    @Transactional
    public Transaction createTransaction(String email, TransactionInput input) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        Account account = lookups.requireActiveAccount(userId, input.accountId());
        CurrencyCode currency = lookups.requireCurrency(profile, input.currency());
        lookups.requireSameCurrency(account.currency(), currency);
        EntryType type = publicEntryType(input.type());
        EntryStatus status = enumValue(EntryStatus.class, input.status(), "status");
        BigDecimal amount = parseMoney(input.amount(), true, "amount");
        String description = requireText(input.description(), "description", 200);
        String category = optionalText(input.category(), "category", 80);
        LocalDate date = requireDate(input.date(), "date");
        String key = requireKey(input.idempotencyKey());

        Optional<Transaction> existing = store.findTransactionByIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            if (!sameTransaction(existing.get(), account.id(), type, status, amount, currency,
                    description, category, date)) {
                throw lookups.idempotencyConflict();
            }
            return existing.get();
        }
        return store.createTransaction(userId, account.id(), type, status, amount, currency,
                description, category, date, RecordSource.MANUAL, null, null, key,
                null, null, null, null);
    }

    @Transactional
    public Transaction updateTransaction(String email, long transactionId, TransactionInput input) {
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

    @Transactional
    public Transaction confirmTransaction(String email, long transactionId) {
        long userId = lookups.userId(email);
        Transaction current = lookups.requireTransaction(userId, transactionId);
        if (current.status() == EntryStatus.REALIZED) {
            return current;
        }
        return store.updateTransaction(userId, current.id(), current.accountId(), current.type(),
                EntryStatus.REALIZED, current.amount(), current.currency(), current.description(),
                current.category(), current.date());
    }

    @Transactional
    public void deleteTransaction(String email, long transactionId) {
        long userId = lookups.userId(email);
        Transaction current = lookups.requireTransaction(userId, transactionId);
        if (current.transferId() != null || current.invoiceId() != null) {
            throw new HealthConflictException("SYSTEM_ENTRY_IMMUTABLE",
                    "Transferências e pagamentos de fatura não podem ser excluídos como lançamentos isolados.");
        }
        store.softDeleteTransaction(userId, transactionId);
    }

    @Transactional
    public TransferView createTransfer(String email, TransferInput input) {
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

    public List<Recurrence> listRecurrences(String email) {
        long userId = lookups.userId(email);
        lookups.requireProfile(userId);
        return store.listRecurrences(userId);
    }

    @Transactional
    public Recurrence createRecurrence(String email, RecurrenceInput input) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        Account account = lookups.requireActiveAccount(userId, input.accountId());
        CurrencyCode currency = lookups.requireCurrency(profile, input.currency());
        lookups.requireSameCurrency(account.currency(), currency);
        EntryType type = publicEntryType(input.type());
        BigDecimal amount = parseMoney(input.amount(), true, "amount");
        int day = requireDay(input.dayOfMonth(), "dayOfMonth");
        LocalDate start = requireDate(input.startDate(), "startDate");
        if (input.endDate() != null && input.endDate().isBefore(start)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        String key = requireKey(input.idempotencyKey());
        String description = requireText(input.description(), "description", 200);
        String category = optionalText(input.category(), "category", 80);

        Optional<Recurrence> existing = store.findRecurrenceByIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            Recurrence r = existing.get();
            if (!sameRecurrence(r, account.id(), type, amount, currency, description, category,
                    day, start, input.endDate())) {
                throw lookups.idempotencyConflict();
            }
            return r;
        }
        Recurrence recurrence = store.createRecurrence(userId, account.id(), type, amount, currency,
                description, category, day, start, input.endDate(), key);
        materializeRecurrence(userId, recurrence, YearMonth.now());
        return recurrence;
    }

    @Transactional
    public Recurrence updateRecurrence(String email, long recurrenceId, RecurrenceInput input) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        lookups.requireRecurrence(userId, recurrenceId);
        Account account = lookups.requireActiveAccount(userId, input.accountId());
        CurrencyCode currency = lookups.requireCurrency(profile, input.currency());
        lookups.requireSameCurrency(account.currency(), currency);
        EntryType type = publicEntryType(input.type());
        BigDecimal amount = parseMoney(input.amount(), true, "amount");
        int day = requireDay(input.dayOfMonth(), "dayOfMonth");
        LocalDate start = requireDate(input.startDate(), "startDate");
        if (input.endDate() != null && input.endDate().isBefore(start)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        String description = requireText(input.description(), "description", 200);
        String category = optionalText(input.category(), "category", 80);
        Recurrence updated = store.updateRecurrence(userId, recurrenceId, account.id(), type, amount,
                currency, description, category, day, start, input.endDate());

        LocalDate futureBoundary = LocalDate.now().withDayOfMonth(1);
        for (Transaction occurrence : store.listTransactions(userId)) {
            if (!Objects.equals(occurrence.recurrenceId(), recurrenceId)
                    || occurrence.status() != EntryStatus.PLANNED
                    || occurrence.date().isBefore(futureBoundary)) {
                continue;
            }
            YearMonth occurrenceMonth = occurrence.recurrenceMonth();
            if (occurrenceMonth == null || !monthIsWithin(updated, occurrenceMonth)) {
                store.softDeleteTransaction(userId, occurrence.id());
            } else {
                LocalDate due = clampedDate(occurrenceMonth, day);
                store.updateTransaction(userId, occurrence.id(), account.id(), type, EntryStatus.PLANNED,
                        amount, currency, description, category, due);
            }
        }
        materializeRecurrence(userId, updated, YearMonth.now());
        return updated;
    }

    @Transactional
    public void deactivateRecurrence(String email, long recurrenceId) {
        long userId = lookups.userId(email);
        lookups.requireRecurrence(userId, recurrenceId);
        store.deactivateRecurrence(userId, recurrenceId);
        store.deletePlannedRecurrenceOccurrencesFrom(userId, recurrenceId, LocalDate.now());
    }

    public List<Card> listCards(String email) {
        long userId = lookups.userId(email);
        lookups.requireProfile(userId);
        return store.listCards(userId);
    }

    @Transactional
    public Card createCard(String email, CardInput input) {
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

    @Transactional
    public Card updateCard(String email, long cardId, CardInput input) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        lookups.requireCard(userId, cardId);
        CurrencyCode currency = lookups.requireCurrency(profile, input.currency());
        return store.updateCard(userId, cardId, requireText(input.name(), "name", 100), currency,
                requireDay(input.closingDay(), "closingDay"), requireDay(input.dueDay(), "dueDay"));
    }

    @Transactional
    public void archiveCard(String email, long cardId) {
        long userId = lookups.userId(email);
        lookups.requireCard(userId, cardId);
        store.archiveCard(userId, cardId);
    }

    @Transactional
    public PurchaseWithInstallments createPurchase(String email, long cardId, PurchaseInput input) {
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
            Invoice invoice = getOrCreateInvoice(userId, card, cycle);
            store.createInstallment(userId, purchase.id(), invoice.id(), currency,
                    index + 1, split.size(), split.get(index));
        }
        return new PurchaseWithInstallments(purchase, store.listInstallmentsByPurchase(userId, purchase.id()));
    }

    public List<InvoiceWithTotal> listInvoices(String email, long cardId) {
        long userId = lookups.userId(email);
        lookups.requireCard(userId, cardId);
        return store.listInvoices(userId, cardId).stream()
                .map(i -> new InvoiceWithTotal(i, money(store.invoiceTotal(userId, i.id()))))
                .toList();
    }

    @Transactional
    public InvoiceWithTotal payInvoice(String email, long invoiceId, InvoicePaymentInput input) {
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

    @Transactional
    public MonthlySummary summary(String email, YearMonth month) {
        long userId = lookups.userId(email);
        Profile profile = lookups.requireProfile(userId);
        YearMonth requestedMonth = month == null ? YearMonth.now() : month;
        materializeRecurrences(userId, requestedMonth);

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

    private void materializeRecurrences(long userId, YearMonth through) {
        for (Recurrence recurrence : store.listRecurrences(userId)) {
            if (recurrence.active()) {
                materializeRecurrence(userId, recurrence, through);
            }
        }
    }

    private void materializeRecurrence(long userId, Recurrence recurrence, YearMonth through) {
        YearMonth cursor = YearMonth.from(recurrence.startDate());
        int generatedOrVisited = 0;
        while (!cursor.isAfter(through)) {
            if (++generatedOrVisited > MAX_RECURRENCE_MONTHS_PER_CALL) {
                throw new IllegalArgumentException("Recurrence range exceeds 240 months");
            }
            if (monthIsWithin(recurrence, cursor)) {
                String key = "recurrence:" + recurrence.id() + ":" + cursor;
                if (store.findTransactionByIdempotencyKey(userId, key).isEmpty()) {
                    LocalDate due = clampedDate(cursor, recurrence.dayOfMonth());
                    store.createTransaction(userId, recurrence.accountId(), recurrence.type(), EntryStatus.PLANNED,
                            recurrence.amount(), recurrence.currency(), recurrence.description(), recurrence.category(),
                            due, RecordSource.SYSTEM, null, null, key, null, recurrence.id(), cursor, null);
                }
            }
            cursor = cursor.plusMonths(1);
        }
    }

    private boolean monthIsWithin(Recurrence recurrence, YearMonth month) {
        LocalDate due = clampedDate(month, recurrence.dayOfMonth());
        return !due.isBefore(recurrence.startDate())
                && (recurrence.endDate() == null || !due.isAfter(recurrence.endDate()));
    }

    private Invoice getOrCreateInvoice(long userId, Card card, YearMonth cycle) {
        return store.findInvoiceByCardAndCycle(userId, card.id(), cycle)
                .orElseGet(() -> {
                    LocalDate closing = clampedDate(cycle, card.closingDay());
                    YearMonth dueMonth = cycle;
                    LocalDate due = clampedDate(dueMonth, card.dueDay());
                    if (!due.isAfter(closing)) {
                        dueMonth = dueMonth.plusMonths(1);
                        due = clampedDate(dueMonth, card.dueDay());
                    }
                    return store.createInvoice(userId, card.id(), card.currency(), cycle, closing, due);
                });
    }


    private BigDecimal sumTransactions(List<Transaction> transactions, EntryType type, EntryStatus status) {
        return transactions.stream().filter(t -> t.type() == type && t.status() == status)
                .map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }


}
