package com.jf.PetApp.application.health;

import static com.jf.PetApp.core.domain.health.HealthModels.*;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.health.exception.HealthConflictException;
import com.jf.PetApp.application.health.port.HealthStore;
import com.jf.PetApp.application.user.port.UserRepository;
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
    private final UserRepository userRepository;

    public HealthService(HealthStore store, UserRepository userRepository) {
        this.store = store;
        this.userRepository = userRepository;
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
        long userId = userId(email);
        return store.findProfile(userId).map(p -> new ProfileView(p, !store.hasFinancialData(userId)));
    }

    @Transactional
    public ProfileView saveProfile(String email, ProfileInput input) {
        long userId = userId(email);
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
        long userId = userId(email);
        requireProfile(userId);
        return store.listAccounts(userId).stream()
                .map(a -> new AccountView(a, money(store.accountBalance(userId, a))))
                .toList();
    }

    @Transactional
    public AccountView createAccount(String email, AccountInput input) {
        long userId = userId(email);
        Profile profile = requireProfile(userId);
        CurrencyCode currency = requireCurrency(profile, input.currency());
        String key = requireKey(input.idempotencyKey());
        BigDecimal initialBalance = parseMoney(input.initialBalance(), false, "initialBalance");
        String name = requireText(input.name(), "name", 100);
        AccountType type = enumValue(AccountType.class, input.type(), "type");
        LocalDate referenceDate = requireDate(input.balanceReferenceDate(), "balanceReferenceDate");

        Optional<Account> existing = store.findAccountByIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            Account account = existing.get();
            if (!sameAccount(account, name, type, initialBalance, referenceDate, currency)) {
                throw idempotencyConflict();
            }
            return new AccountView(account, money(store.accountBalance(userId, account)));
        }
        Account account = store.createAccount(userId, name, type, initialBalance, referenceDate, currency, key);
        return new AccountView(account, money(store.accountBalance(userId, account)));
    }

    @Transactional
    public AccountView updateAccount(String email, long accountId, AccountInput input) {
        long userId = userId(email);
        Profile profile = requireProfile(userId);
        Account current = requireAccount(userId, accountId);
        CurrencyCode currency = requireCurrency(profile, input.currency());
        if (current.currency() != currency) {
            throw currencyMismatch(profile.primaryCurrency(), currency);
        }
        Account updated = store.updateAccount(userId, accountId,
                requireText(input.name(), "name", 100), enumValue(AccountType.class, input.type(), "type"),
                parseMoney(input.initialBalance(), false, "initialBalance"),
                requireDate(input.balanceReferenceDate(), "balanceReferenceDate"), currency);
        return new AccountView(updated, money(store.accountBalance(userId, updated)));
    }

    @Transactional
    public void archiveAccount(String email, long accountId) {
        long userId = userId(email);
        requireAccount(userId, accountId);
        store.archiveAccount(userId, accountId);
    }

    public List<Transaction> listTransactions(String email, TransactionFilter filter) {
        long userId = userId(email);
        requireProfile(userId);
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
        long userId = userId(email);
        Profile profile = requireProfile(userId);
        Account account = requireActiveAccount(userId, input.accountId());
        CurrencyCode currency = requireCurrency(profile, input.currency());
        requireSameCurrency(account.currency(), currency);
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
                throw idempotencyConflict();
            }
            return existing.get();
        }
        return store.createTransaction(userId, account.id(), type, status, amount, currency,
                description, category, date, RecordSource.MANUAL, null, null, key,
                null, null, null, null);
    }

    @Transactional
    public Transaction updateTransaction(String email, long transactionId, TransactionInput input) {
        long userId = userId(email);
        Profile profile = requireProfile(userId);
        Transaction current = requireTransaction(userId, transactionId);
        if (current.type() == EntryType.TRANSFER_IN || current.type() == EntryType.TRANSFER_OUT
                || current.type() == EntryType.INVOICE_PAYMENT) {
            throw new HealthConflictException("SYSTEM_ENTRY_IMMUTABLE",
                    "Transferências e pagamentos de fatura devem ser alterados pelo fluxo que os criou.");
        }
        Account account = requireActiveAccount(userId, input.accountId());
        CurrencyCode currency = requireCurrency(profile, input.currency());
        requireSameCurrency(account.currency(), currency);
        return store.updateTransaction(userId, transactionId, account.id(), publicEntryType(input.type()),
                enumValue(EntryStatus.class, input.status(), "status"),
                parseMoney(input.amount(), true, "amount"), currency,
                requireText(input.description(), "description", 200),
                optionalText(input.category(), "category", 80), requireDate(input.date(), "date"));
    }

    @Transactional
    public Transaction confirmTransaction(String email, long transactionId) {
        long userId = userId(email);
        Transaction current = requireTransaction(userId, transactionId);
        if (current.status() == EntryStatus.REALIZED) {
            return current;
        }
        return store.updateTransaction(userId, current.id(), current.accountId(), current.type(),
                EntryStatus.REALIZED, current.amount(), current.currency(), current.description(),
                current.category(), current.date());
    }

    @Transactional
    public void deleteTransaction(String email, long transactionId) {
        long userId = userId(email);
        Transaction current = requireTransaction(userId, transactionId);
        if (current.transferId() != null || current.invoiceId() != null) {
            throw new HealthConflictException("SYSTEM_ENTRY_IMMUTABLE",
                    "Transferências e pagamentos de fatura não podem ser excluídos como lançamentos isolados.");
        }
        store.softDeleteTransaction(userId, transactionId);
    }

    @Transactional
    public TransferView createTransfer(String email, TransferInput input) {
        long userId = userId(email);
        Profile profile = requireProfile(userId);
        Account from = requireActiveAccount(userId, input.fromAccountId());
        Account to = requireActiveAccount(userId, input.toAccountId());
        if (from.id() == to.id()) {
            throw new IllegalArgumentException("fromAccountId and toAccountId must be different");
        }
        CurrencyCode currency = requireCurrency(profile, input.currency());
        requireSameCurrency(from.currency(), currency);
        requireSameCurrency(to.currency(), currency);
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
                throw idempotencyConflict();
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
        long userId = userId(email);
        requireProfile(userId);
        return store.listRecurrences(userId);
    }

    @Transactional
    public Recurrence createRecurrence(String email, RecurrenceInput input) {
        long userId = userId(email);
        Profile profile = requireProfile(userId);
        Account account = requireActiveAccount(userId, input.accountId());
        CurrencyCode currency = requireCurrency(profile, input.currency());
        requireSameCurrency(account.currency(), currency);
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
                throw idempotencyConflict();
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
        long userId = userId(email);
        Profile profile = requireProfile(userId);
        requireRecurrence(userId, recurrenceId);
        Account account = requireActiveAccount(userId, input.accountId());
        CurrencyCode currency = requireCurrency(profile, input.currency());
        requireSameCurrency(account.currency(), currency);
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
        long userId = userId(email);
        requireRecurrence(userId, recurrenceId);
        store.deactivateRecurrence(userId, recurrenceId);
        store.deletePlannedRecurrenceOccurrencesFrom(userId, recurrenceId, LocalDate.now());
    }

    public List<Card> listCards(String email) {
        long userId = userId(email);
        requireProfile(userId);
        return store.listCards(userId);
    }

    @Transactional
    public Card createCard(String email, CardInput input) {
        long userId = userId(email);
        Profile profile = requireProfile(userId);
        CurrencyCode currency = requireCurrency(profile, input.currency());
        String name = requireText(input.name(), "name", 100);
        int closingDay = requireDay(input.closingDay(), "closingDay");
        int dueDay = requireDay(input.dueDay(), "dueDay");
        String key = requireKey(input.idempotencyKey());
        Optional<Card> existing = store.findCardByIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            Card card = existing.get();
            if (!card.name().equals(name) || card.currency() != currency
                    || card.closingDay() != closingDay || card.dueDay() != dueDay) {
                throw idempotencyConflict();
            }
            return card;
        }
        return store.createCard(userId, name, currency, closingDay, dueDay, key);
    }

    @Transactional
    public Card updateCard(String email, long cardId, CardInput input) {
        long userId = userId(email);
        Profile profile = requireProfile(userId);
        requireCard(userId, cardId);
        CurrencyCode currency = requireCurrency(profile, input.currency());
        return store.updateCard(userId, cardId, requireText(input.name(), "name", 100), currency,
                requireDay(input.closingDay(), "closingDay"), requireDay(input.dueDay(), "dueDay"));
    }

    @Transactional
    public void archiveCard(String email, long cardId) {
        long userId = userId(email);
        requireCard(userId, cardId);
        store.archiveCard(userId, cardId);
    }

    @Transactional
    public PurchaseWithInstallments createPurchase(String email, long cardId, PurchaseInput input) {
        long userId = userId(email);
        Profile profile = requireProfile(userId);
        Card card = requireActiveCard(userId, cardId);
        CurrencyCode currency = requireCurrency(profile, input.currency());
        requireSameCurrency(card.currency(), currency);
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
                throw idempotencyConflict();
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
        long userId = userId(email);
        requireCard(userId, cardId);
        return store.listInvoices(userId, cardId).stream()
                .map(i -> new InvoiceWithTotal(i, money(store.invoiceTotal(userId, i.id()))))
                .toList();
    }

    @Transactional
    public InvoiceWithTotal payInvoice(String email, long invoiceId, InvoicePaymentInput input) {
        long userId = userId(email);
        Profile profile = requireProfile(userId);
        Invoice invoice = requireInvoice(userId, invoiceId);
        Account account = requireActiveAccount(userId, input.accountId());
        CurrencyCode currency = requireCurrency(profile, input.currency());
        requireSameCurrency(invoice.currency(), currency);
        requireSameCurrency(account.currency(), currency);
        LocalDate paymentDate = requireDate(input.paymentDate(), "paymentDate");
        String key = requireKey(input.idempotencyKey());

        Optional<Transaction> existingPayment = store.findTransactionByIdempotencyKey(userId, key);
        if (existingPayment.isPresent()) {
            if (!Objects.equals(existingPayment.get().invoiceId(), invoiceId)
                    || existingPayment.get().accountId() != account.id()) {
                throw idempotencyConflict();
            }
            Invoice current = requireInvoice(userId, invoiceId);
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
        long userId = userId(email);
        Profile profile = requireProfile(userId);
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

    static YearMonth firstInvoiceCycle(Card card, LocalDate purchaseDate) {
        YearMonth purchaseMonth = YearMonth.from(purchaseDate);
        LocalDate closing = clampedDate(purchaseMonth, card.closingDay());
        return purchaseDate.isAfter(closing) ? purchaseMonth.plusMonths(1) : purchaseMonth;
    }

    static LocalDate clampedDate(YearMonth month, int requestedDay) {
        return month.atDay(Math.min(requestedDay, month.lengthOfMonth()));
    }

    static List<BigDecimal> splitInstallments(BigDecimal total, int count) {
        if (total == null || total.scale() > 2 || total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("total must be a positive scale-2 monetary value");
        }
        if (count < 1 || count > 120) {
            throw new IllegalArgumentException("installmentCount must be between 1 and 120");
        }
        BigInteger minor = total.movePointRight(2).toBigIntegerExact();
        BigInteger[] division = minor.divideAndRemainder(BigInteger.valueOf(count));
        List<BigDecimal> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BigInteger cents = division[0].add(i < division[1].intValueExact() ? BigInteger.ONE : BigInteger.ZERO);
            values.add(new BigDecimal(cents, 2));
        }
        return List.copyOf(values);
    }

    private BigDecimal sumTransactions(List<Transaction> transactions, EntryType type, EntryStatus status) {
        return transactions.stream().filter(t -> t.type() == type && t.status() == status)
                .map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long userId(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getId() != null)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }

    private Profile requireProfile(long userId) {
        return store.findProfile(userId).orElseThrow(() ->
                new ResourceNotFoundException("Health profile not found. Complete Health onboarding first."));
    }

    private Account requireAccount(long userId, long accountId) {
        return store.findAccount(userId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private Account requireActiveAccount(long userId, long accountId) {
        Account account = requireAccount(userId, accountId);
        if (account.archived()) {
            throw new HealthConflictException("ACCOUNT_ARCHIVED", "A conta está arquivada.");
        }
        return account;
    }

    private Transaction requireTransaction(long userId, long transactionId) {
        return store.findTransaction(userId, transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    private Recurrence requireRecurrence(long userId, long recurrenceId) {
        return store.findRecurrence(userId, recurrenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurrence not found"));
    }

    private Card requireCard(long userId, long cardId) {
        return store.findCard(userId, cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    }

    private Card requireActiveCard(long userId, long cardId) {
        Card card = requireCard(userId, cardId);
        if (card.archived()) {
            throw new HealthConflictException("CARD_ARCHIVED", "O cartão está arquivado.");
        }
        return card;
    }

    private Invoice requireInvoice(long userId, long invoiceId) {
        return store.findInvoice(userId, invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
    }

    private CurrencyCode requireCurrency(Profile profile, String requested) {
        CurrencyCode currency = enumValue(CurrencyCode.class, requested, "currency");
        if (profile.primaryCurrency() != currency) {
            throw currencyMismatch(profile.primaryCurrency(), currency);
        }
        return currency;
    }

    private void requireSameCurrency(CurrencyCode expected, CurrencyCode actual) {
        if (expected != actual) {
            throw currencyMismatch(expected, actual);
        }
    }

    private HealthConflictException currencyMismatch(CurrencyCode expected, CurrencyCode actual) {
        return new HealthConflictException("CURRENCY_MISMATCH",
                "A moeda do registro (" + actual + ") não corresponde à moeda principal (" + expected + ").");
    }

    private HealthConflictException idempotencyConflict() {
        return new HealthConflictException("IDEMPOTENCY_KEY_REUSED",
                "A chave de idempotência já foi usada com dados diferentes.");
    }

    private static BigDecimal parseMoney(String raw, boolean positive, String field) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(field + " is required as a decimal string");
        }
        final BigDecimal parsed;
        try {
            parsed = new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be a decimal string");
        }
        BigDecimal stripped = parsed.stripTrailingZeros();
        if (stripped.scale() > 2) {
            throw new IllegalArgumentException(field + " must have at most 2 decimal places");
        }
        BigDecimal value = parsed.setScale(2, RoundingMode.UNNECESSARY);
        if (value.precision() > 19) {
            throw new IllegalArgumentException(field + " is too large");
        }
        if (positive && value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported " + field + ": " + raw);
        }
    }

    private static EntryType publicEntryType(String raw) {
        EntryType type = enumValue(EntryType.class, raw, "type");
        if (type != EntryType.INCOME && type != EntryType.EXPENSE) {
            throw new IllegalArgumentException("type must be INCOME or EXPENSE");
        }
        return type;
    }

    private static String requireLocale(String locale) {
        if (!"pt-BR".equals(locale) && !"pt-PT".equals(locale)) {
            throw new IllegalArgumentException("localeTag must be 'pt-BR' or 'pt-PT'");
        }
        return locale;
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(field + " must have at most " + maxLength + " characters");
        }
        return trimmed;
    }

    private static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireText(value, field, maxLength);
    }

    private static LocalDate requireDate(LocalDate date, String field) {
        if (date == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return date;
    }

    private static int requireDay(int value, String field) {
        if (value < 1 || value > 31) {
            throw new IllegalArgumentException(field + " must be between 1 and 31");
        }
        return value;
    }

    private static String requireKey(String key) {
        return requireText(key, "idempotencyKey", 64);
    }

    private static String normalizeCategory(String category) {
        return category == null ? null : category.trim().toLowerCase(Locale.ROOT);
    }

    private static String categoryOrOther(String category) {
        return category == null || category.isBlank() ? "other" : category;
    }

    private static boolean sameAccount(Account a, String name, AccountType type, BigDecimal initial,
                                       LocalDate date, CurrencyCode currency) {
        return a.name().equals(name) && a.type() == type && a.initialBalance().compareTo(initial) == 0
                && a.balanceReferenceDate().equals(date) && a.currency() == currency;
    }

    private static boolean sameTransaction(Transaction t, long accountId, EntryType type, EntryStatus status,
                                           BigDecimal amount, CurrencyCode currency, String description,
                                           String category, LocalDate date) {
        return t.accountId() == accountId && t.type() == type && t.status() == status
                && t.amount().compareTo(amount) == 0 && t.currency() == currency
                && t.description().equals(description) && Objects.equals(t.category(), category)
                && t.date().equals(date) && t.deletedAt() == null;
    }

    private static boolean sameRecurrence(Recurrence r, long accountId, EntryType type, BigDecimal amount,
                                          CurrencyCode currency, String description, String category, int day,
                                          LocalDate start, LocalDate end) {
        return r.accountId() == accountId && r.type() == type && r.amount().compareTo(amount) == 0
                && r.currency() == currency && r.description().equals(description)
                && Objects.equals(r.category(), category) && r.dayOfMonth() == day
                && r.startDate().equals(start) && Objects.equals(r.endDate(), end);
    }
}
