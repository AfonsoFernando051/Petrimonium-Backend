package com.jf.PetApp.application.health;

import static com.jf.PetApp.core.domain.health.HealthModels.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.health.exception.HealthConflictException;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.RoleEnum;
import com.jf.PetApp.infrastructure.repository.health.JdbcHealthStore;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Health rules exercised end to end against the real migrations on H2 — the same files
 * {@code spring-boot:run} applies in dev — rather than against a mocked store. Balances, invoice
 * cycles and idempotency all live partly in SQL (check constraints, unique keys, the balance sum),
 * so a mocked {@code HealthStore} would prove almost nothing about them.
 *
 * <p>Two deliberate limits: {@code @Transactional} does nothing here (there is no Spring proxy),
 * so this suite verifies the outcomes of each operation, not rollback; and every date is derived
 * from {@code YearMonth.now()} so the assertions do not silently change meaning on the first of
 * a month.
 */
class HealthServiceIntegrationTest {

    private static final String ANA = "ana@example.com";
    private static final String BRUNO = "bruno@example.com";

    private SingleConnectionDataSource dataSource;
    private JdbcTemplate jdbc;
    private JdbcHealthStore store;
    private HealthService service;
    private final Map<String, User> users = new HashMap<>();

    /** Emails map to the {@code jf_users} rows this test inserted; nothing else is looked up. */
    private final UserRepository userRepository = new UserRepository() {
        @Override public Optional<User> findById(int id) { return Optional.empty(); }
        @Override public Optional<User> findByEmail(String email) {
            return Optional.ofNullable(users.get(email));
        }
        @Override public Optional<User> findByUsername(String username) { return Optional.empty(); }
        @Override public Optional<User> findByProviderId(String providerId) { return Optional.empty(); }
        @Override public User save(User user) { return user; }
    };

    @BeforeEach
    void setUp() {
        String url = "jdbc:h2:mem:health-" + System.nanoTime();
        dataSource = new SingleConnectionDataSource(url, "sa", "", true);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        store = new JdbcHealthStore(jdbc, new MockEnvironment());
        service = new HealthService(store, userRepository);
        registerUser(ANA);
        registerUser(BRUNO);
    }

    @AfterEach
    void tearDown() {
        dataSource.destroy();
    }

    private void registerUser(String email) {
        jdbc.update("insert into jf_users (email, password, is_active) values (?, 'x', true)", email);
        Long id = jdbc.queryForObject("select user_id from jf_users where email = ?", Long.class, email);
        User user = User.create(email, email, "x", RoleEnum.USER);
        user.setId(id);
        users.put(email, user);
    }

    // ------------------------------------------------------------------ helpers

    private static YearMonth thisMonth() {
        return YearMonth.now();
    }

    private static LocalDate day(int dayOfMonth) {
        return HealthService.clampedDate(thisMonth(), dayOfMonth);
    }

    private void onboard(String email, CountryCode country, CurrencyCode currency, String locale) {
        service.saveProfile(email, new HealthService.ProfileInput(country.name(), currency.name(), locale));
    }

    private long account(String email, String name, String initialBalance, CurrencyCode currency) {
        return service.createAccount(email, new HealthService.AccountInput(name, "CHECKING", initialBalance,
                thisMonth().atDay(1), currency.name(), "acc-" + email + "-" + name)).account().id();
    }

    private BigDecimal balance(String email, long accountId) {
        return service.listAccounts(email).stream()
                .filter(view -> view.account().id() == accountId)
                .findFirst().orElseThrow().currentBalance();
    }

    private Transaction entry(String email, long accountId, String type, String status, String amount,
                              CurrencyCode currency, String category, LocalDate date, String key) {
        return service.createTransaction(email, new HealthService.TransactionInput(accountId, type, status,
                amount, currency.name(), "Lançamento " + key, category, date, key));
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    private static String code(HealthConflictException e) {
        return e.code();
    }

    // ------------------------------------------------------------- user isolation

    @Test
    void aUserNeverSeesOrTouchesAnotherUsersData() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        onboard(BRUNO, CountryCode.PT, CurrencyCode.EUR, "pt-PT");
        long anaAccount = account(ANA, "Corrente", "500.00", CurrencyCode.BRL);
        entry(ANA, anaAccount, "EXPENSE", "REALIZED", "20.00", CurrencyCode.BRL, "food", day(5), "ana-1");

        assertTrue(service.listAccounts(BRUNO).isEmpty(), "Bruno's account list must not leak Ana's account");
        assertTrue(service.listTransactions(BRUNO, null).isEmpty());

        // Bruno holds a valid session and guesses Ana's account id: 404, the same answer he would
        // get for an id that does not exist at all, so he cannot even confirm it is real.
        assertThrows(ResourceNotFoundException.class, () -> service.archiveAccount(BRUNO, anaAccount));
        assertThrows(ResourceNotFoundException.class, () -> service.updateAccount(BRUNO, anaAccount,
                new HealthService.AccountInput("Roubada", "CHECKING", "1.00", thisMonth().atDay(1), "EUR", null)));
        assertMoney("480.00", balance(ANA, anaAccount));
    }

    @Test
    void summaryTotalsOnlyCoverTheAuthenticatedUser() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        onboard(BRUNO, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long anaAccount = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);
        long brunoAccount = account(BRUNO, "Corrente", "7000.00", CurrencyCode.BRL);
        entry(ANA, anaAccount, "INCOME", "REALIZED", "100.00", CurrencyCode.BRL, "salary", day(2), "ana-in");
        entry(BRUNO, brunoAccount, "INCOME", "REALIZED", "9000.00", CurrencyCode.BRL, "salary", day(2), "bruno-in");

        assertMoney("1100.00", service.summary(ANA, thisMonth()).currentBalance());
        assertMoney("100.00", service.summary(ANA, thisMonth()).realizedIncome());
    }

    // -------------------------------------------------------- balances and status

    @Test
    void theOpeningBalanceIsAStartingPointAndNotIncome() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "1500.00", CurrencyCode.BRL);

        MonthlySummary summary = service.summary(ANA, thisMonth());

        assertMoney("1500.00", summary.currentBalance());
        assertMoney("0.00", summary.realizedIncome());
        assertMoney("0.00", summary.monthResult());
        assertMoney("1500.00", balance(ANA, id));
    }

    @Test
    void aPlannedEntryDoesNotMoveTheBalanceAndConfirmingItDoesNotDuplicateTheEntry() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);
        Transaction planned = entry(ANA, id, "EXPENSE", "PLANNED", "250.00", CurrencyCode.BRL, "housing",
                day(20), "rent");

        assertMoney("1000.00", balance(ANA, id));
        assertMoney("250.00", service.summary(ANA, thisMonth()).plannedExpenses());

        Transaction confirmed = service.confirmTransaction(ANA, planned.id());

        assertEquals(planned.id(), confirmed.id(), "confirming must update the entry, never write a second one");
        assertEquals(EntryStatus.REALIZED, confirmed.status());
        assertEquals(1, service.listTransactions(ANA, null).size());
        assertMoney("750.00", balance(ANA, id));
        assertMoney("0.00", service.summary(ANA, thisMonth()).plannedExpenses());
    }

    @Test
    void confirmingAnAlreadyConfirmedEntryChangesNothing() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);
        Transaction realized = entry(ANA, id, "EXPENSE", "REALIZED", "40.00", CurrencyCode.BRL, "food",
                day(3), "lunch");

        service.confirmTransaction(ANA, realized.id());
        service.confirmTransaction(ANA, realized.id());

        assertEquals(1, service.listTransactions(ANA, null).size());
        assertMoney("960.00", balance(ANA, id));
    }

    @Test
    void repeatingACreateWithTheSameIdempotencyKeyDoesNotChargeTwice() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);

        Transaction first = entry(ANA, id, "EXPENSE", "REALIZED", "30.00", CurrencyCode.BRL, "food",
                day(4), "retry-key");
        Transaction retry = entry(ANA, id, "EXPENSE", "REALIZED", "30.00", CurrencyCode.BRL, "food",
                day(4), "retry-key");

        assertEquals(first.id(), retry.id());
        assertEquals(1, service.listTransactions(ANA, null).size());
        assertMoney("970.00", balance(ANA, id));
    }

    @Test
    void reusingAnIdempotencyKeyForDifferentDataIsRejected() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);
        entry(ANA, id, "EXPENSE", "REALIZED", "30.00", CurrencyCode.BRL, "food", day(4), "key");

        HealthConflictException e = assertThrows(HealthConflictException.class,
                () -> entry(ANA, id, "EXPENSE", "REALIZED", "3000.00", CurrencyCode.BRL, "food", day(4), "key"));

        assertEquals("IDEMPOTENCY_KEY_REUSED", code(e));
    }

    @Test
    void deletingAnEntryRemovesItsEffectOnTheBalance() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);
        Transaction expense = entry(ANA, id, "EXPENSE", "REALIZED", "100.00", CurrencyCode.BRL, "food",
                day(6), "gone");

        service.deleteTransaction(ANA, expense.id());

        assertMoney("1000.00", balance(ANA, id));
        assertTrue(service.listTransactions(ANA, null).isEmpty());
    }

    @Test
    void archivingAnAccountKeepsItsHistoryAndBalance() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Antiga", "300.00", CurrencyCode.BRL);
        entry(ANA, id, "EXPENSE", "REALIZED", "50.00", CurrencyCode.BRL, "food", day(2), "old");

        service.archiveAccount(ANA, id);

        HealthService.AccountView archived = service.listAccounts(ANA).stream()
                .filter(view -> view.account().id() == id).findFirst().orElseThrow();
        assertTrue(archived.account().archived());
        assertMoney("250.00", archived.currentBalance());
        assertEquals(1, service.listTransactions(ANA, null).size());
        // An archived account no longer accepts new entries, so it cannot silently keep collecting.
        assertEquals("ACCOUNT_ARCHIVED", code(assertThrows(HealthConflictException.class,
                () -> entry(ANA, id, "EXPENSE", "REALIZED", "10.00", CurrencyCode.BRL, "food", day(3), "after"))));
    }

    // --------------------------------------------------------------- transfers

    @Test
    void aTransferMovesMoneyWithoutCountingAsIncomeOrExpense() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long checking = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);
        long savings = account(ANA, "Poupança", "200.00", CurrencyCode.BRL);

        service.createTransfer(ANA, new HealthService.TransferInput(checking, savings, "300.00", "BRL",
                day(10), "Reserva", "tr-1"));

        assertMoney("700.00", balance(ANA, checking));
        assertMoney("500.00", balance(ANA, savings));

        MonthlySummary summary = service.summary(ANA, thisMonth());
        assertMoney("0.00", summary.realizedIncome());
        assertMoney("0.00", summary.realizedExpenses());
        assertMoney("0.00", summary.monthResult());
        assertMoney("1200.00", summary.currentBalance());
    }

    @Test
    void aRepeatedTransferRequestIsAppliedOnlyOnce() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long checking = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);
        long savings = account(ANA, "Poupança", "0.00", CurrencyCode.BRL);
        HealthService.TransferInput input = new HealthService.TransferInput(checking, savings, "300.00",
                "BRL", day(10), "Reserva", "same-key");

        HealthService.TransferView first = service.createTransfer(ANA, input);
        HealthService.TransferView retry = service.createTransfer(ANA, input);

        assertEquals(first.transfer().id(), retry.transfer().id());
        assertMoney("700.00", balance(ANA, checking));
        assertMoney("300.00", balance(ANA, savings));
        assertEquals(2, service.listTransactions(ANA, null).size(), "one transfer means exactly two legs");
    }

    @Test
    void aTransferLegCannotBeEditedOrDeletedAsIfItWereAnOrdinaryEntry() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long checking = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);
        long savings = account(ANA, "Poupança", "0.00", CurrencyCode.BRL);
        HealthService.TransferView transfer = service.createTransfer(ANA, new HealthService.TransferInput(
                checking, savings, "100.00", "BRL", day(10), "Reserva", "tr-lock"));
        long legId = transfer.outTransaction().id();

        assertEquals("SYSTEM_ENTRY_IMMUTABLE", code(assertThrows(HealthConflictException.class,
                () -> service.deleteTransaction(ANA, legId))));
        assertEquals("SYSTEM_ENTRY_IMMUTABLE", code(assertThrows(HealthConflictException.class,
                () -> service.updateTransaction(ANA, legId, new HealthService.TransactionInput(checking,
                        "EXPENSE", "REALIZED", "1.00", "BRL", "x", null, day(10), null)))));
    }

    @Test
    void aTransferToTheSameAccountIsRejected() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long checking = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);

        assertThrows(IllegalArgumentException.class, () -> service.createTransfer(ANA,
                new HealthService.TransferInput(checking, checking, "10.00", "BRL", day(10), null, "self")));
    }

    @Test
    void aTransferIntoAnotherUsersAccountIsRejected() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        onboard(BRUNO, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long anaAccount = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);
        long brunoAccount = account(BRUNO, "Corrente", "0.00", CurrencyCode.BRL);

        assertThrows(ResourceNotFoundException.class, () -> service.createTransfer(ANA,
                new HealthService.TransferInput(anaAccount, brunoAccount, "10.00", "BRL", day(10), null, "x")));
        assertMoney("1000.00", balance(ANA, anaAccount));
        assertMoney("0.00", balance(BRUNO, brunoAccount));
    }

    // -------------------------------------------------------------- recurrences

    @Test
    void aMonthlyCommitmentFallsOnTheLastDayOfMonthsThatAreTooShort() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "5000.00", CurrencyCode.BRL);
        // Thirteen months back guarantees the generated range crosses one February.
        YearMonth start = thisMonth().minusMonths(13);
        service.createRecurrence(ANA, new HealthService.RecurrenceInput(id, "EXPENSE", "1200.00", "BRL",
                "Aluguel", "housing", 31, start.atDay(1), null, "rec-31"));

        List<Transaction> occurrences = service.listTransactions(ANA,
                new HealthService.TransactionFilter(start.atDay(1), thisMonth().atEndOfMonth(), null, null, null));

        assertFalse(occurrences.isEmpty());
        for (Transaction occurrence : occurrences) {
            YearMonth month = YearMonth.from(occurrence.date());
            assertEquals(Math.min(31, month.lengthOfMonth()), occurrence.date().getDayOfMonth(),
                    "day 31 must clamp to the last day of " + month);
        }
        Transaction february = occurrences.stream()
                .filter(t -> t.date().getMonthValue() == 2).findFirst().orElseThrow();
        assertEquals(february.date().lengthOfMonth(), february.date().getDayOfMonth());
    }

    @Test
    void generatingTheSameMonthAgainDoesNotDuplicateTheCommitment() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "5000.00", CurrencyCode.BRL);
        service.createRecurrence(ANA, new HealthService.RecurrenceInput(id, "EXPENSE", "300.00", "BRL",
                "Internet", "utilities", 10, thisMonth().atDay(1), null, "rec-net"));

        service.summary(ANA, thisMonth());
        service.listTransactions(ANA, null);
        service.summary(ANA, thisMonth());

        long thisMonthOccurrences = service.listTransactions(ANA, null).stream()
                .filter(t -> YearMonth.from(t.date()).equals(thisMonth())).count();
        assertEquals(1, thisMonthOccurrences, "one recurrence produces one occurrence per month, however often it is read");
    }

    @Test
    void aGeneratedOccurrenceIsPlannedUntilTheUserConfirmsIt() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "5000.00", CurrencyCode.BRL);
        service.createRecurrence(ANA, new HealthService.RecurrenceInput(id, "EXPENSE", "300.00", "BRL",
                "Internet", "utilities", 10, thisMonth().atDay(1), null, "rec-net"));

        Transaction occurrence = service.listTransactions(ANA, null).getFirst();
        assertEquals(EntryStatus.PLANNED, occurrence.status());
        assertMoney("5000.00", balance(ANA, id));

        service.confirmTransaction(ANA, occurrence.id());
        assertMoney("4700.00", balance(ANA, id));
    }

    @Test
    void editingARecurrenceKeepsWhatWasAlreadyConfirmed() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "5000.00", CurrencyCode.BRL);
        YearMonth start = thisMonth().minusMonths(2);
        Recurrence recurrence = service.createRecurrence(ANA, new HealthService.RecurrenceInput(id, "EXPENSE",
                "1000.00", "BRL", "Aluguel", "housing", 5, start.atDay(1), null, "rec-rent"));
        Transaction past = service.listTransactions(ANA, null).stream()
                .filter(t -> YearMonth.from(t.date()).equals(start)).findFirst().orElseThrow();
        service.confirmTransaction(ANA, past.id());

        // The rent goes up: future months change, the month already paid does not.
        service.updateRecurrence(ANA, recurrence.id(), new HealthService.RecurrenceInput(id, "EXPENSE",
                "1100.00", "BRL", "Aluguel", "housing", 5, start.atDay(1), null, null));

        Transaction paid = service.listTransactions(ANA, null).stream()
                .filter(t -> t.id() == past.id()).findFirst().orElseThrow();
        assertEquals(EntryStatus.REALIZED, paid.status());
        assertMoney("1000.00", paid.amount());

        Transaction current = service.listTransactions(ANA, null).stream()
                .filter(t -> YearMonth.from(t.date()).equals(thisMonth())).findFirst().orElseThrow();
        assertEquals(EntryStatus.PLANNED, current.status());
        assertMoney("1100.00", current.amount());
    }

    @Test
    void deactivatingARecurrenceDropsFutureOccurrencesAndKeepsPastOnes() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "5000.00", CurrencyCode.BRL);
        YearMonth start = thisMonth().minusMonths(2);
        Recurrence recurrence = service.createRecurrence(ANA, new HealthService.RecurrenceInput(id, "EXPENSE",
                "80.00", "BRL", "Streaming", "leisure", 1, start.atDay(1), null, "rec-stream"));
        Transaction past = service.listTransactions(ANA, null).stream()
                .filter(t -> YearMonth.from(t.date()).equals(start)).findFirst().orElseThrow();
        service.confirmTransaction(ANA, past.id());

        service.deactivateRecurrence(ANA, recurrence.id());

        List<Transaction> remaining = service.listTransactions(ANA, null);
        assertTrue(remaining.stream().anyMatch(t -> t.id() == past.id()), "history must survive cancellation");
        assertTrue(remaining.stream().noneMatch(t -> t.status() == EntryStatus.PLANNED
                        && t.date().isAfter(LocalDate.now())),
                "no occurrence may be generated after the commitment was cancelled");
        assertFalse(service.listRecurrences(ANA).getFirst().active());
    }

    // ------------------------------------------------------- cards and invoices

    private long card(String email, int closingDay, int dueDay, CurrencyCode currency) {
        return service.createCard(email, new HealthService.CardInput("Cartão", currency.name(),
                closingDay, dueDay, "card-" + email)).id();
    }

    @Test
    void anInstallmentPurchaseSpreadsOverConsecutiveInvoicesAndSumsExactly() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long cardId = card(ANA, 20, 28, CurrencyCode.BRL);

        PurchaseWithInstallments purchase = service.createPurchase(ANA, cardId,
                new HealthService.PurchaseInput("100.00", "BRL", "Notebook", "shopping",
                        thisMonth().atDay(1), 3, "buy-1"));

        assertEquals(3, purchase.installments().size());
        assertMoney("100.00", purchase.installments().stream().map(Installment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertMoney("33.34", purchase.installments().getFirst().amount());

        List<InvoiceWithTotal> invoices = service.listInvoices(ANA, cardId);
        assertEquals(3, invoices.size(), "each installment lands on its own monthly invoice");
        assertEquals(List.of(thisMonth(), thisMonth().plusMonths(1), thisMonth().plusMonths(2)),
                invoices.stream().map(i -> i.invoice().cycleMonth()).sorted().toList());
    }

    @Test
    void aPurchaseAfterTheClosingDayLandsOnTheNextInvoice() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long cardId = card(ANA, 10, 20, CurrencyCode.BRL);

        service.createPurchase(ANA, cardId, new HealthService.PurchaseInput("50.00", "BRL", "Livro",
                "shopping", day(11), 1, "buy-late"));

        InvoiceWithTotal invoice = service.listInvoices(ANA, cardId).getFirst();
        assertEquals(thisMonth().plusMonths(1), invoice.invoice().cycleMonth());
        assertMoney("50.00", invoice.total());
    }

    @Test
    void invoicesAreListedOldestCycleFirstSoTheNextOneToPayComesFirst() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long cardId = card(ANA, 25, 5, CurrencyCode.BRL);
        service.createPurchase(ANA, cardId, new HealthService.PurchaseInput("300.00", "BRL", "Curso",
                "education", thisMonth().atDay(1), 3, "buy-order"));

        List<YearMonth> cycles = service.listInvoices(ANA, cardId).stream()
                .map(i -> i.invoice().cycleMonth()).toList();

        assertEquals(List.of(thisMonth(), thisMonth().plusMonths(1), thisMonth().plusMonths(2)), cycles,
                "a card screen must open on the invoice that is due next, not on a future one");
    }

    @Test
    void payingAnInvoiceLeavesTheAccountLowerWithoutRecordingASecondExpense() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long accountId = account(ANA, "Corrente", "2000.00", CurrencyCode.BRL);
        long cardId = card(ANA, 20, 28, CurrencyCode.BRL);
        service.createPurchase(ANA, cardId, new HealthService.PurchaseInput("400.00", "BRL", "Mercado",
                "food", thisMonth().atDay(1), 1, "buy-food"));
        InvoiceWithTotal invoice = service.listInvoices(ANA, cardId).getFirst();

        MonthlySummary beforePayment = service.summary(ANA, thisMonth());
        assertMoney("400.00", beforePayment.realizedExpenses());

        service.payInvoice(ANA, invoice.invoice().id(), new HealthService.InvoicePaymentInput(accountId,
                "BRL", day(28), "pay-1"));

        assertMoney("1600.00", balance(ANA, accountId));
        MonthlySummary afterPayment = service.summary(ANA, thisMonth());
        assertMoney("400.00", afterPayment.realizedExpenses());
        assertMoney("1600.00", afterPayment.currentBalance());
        assertEquals(InvoiceStatus.PAID, service.listInvoices(ANA, cardId).getFirst().invoice().status());
    }

    @Test
    void anInvoiceCannotBePaidTwice() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long accountId = account(ANA, "Corrente", "2000.00", CurrencyCode.BRL);
        long cardId = card(ANA, 20, 28, CurrencyCode.BRL);
        service.createPurchase(ANA, cardId, new HealthService.PurchaseInput("400.00", "BRL", "Mercado",
                "food", thisMonth().atDay(1), 1, "buy-food"));
        long invoiceId = service.listInvoices(ANA, cardId).getFirst().invoice().id();
        HealthService.InvoicePaymentInput payment = new HealthService.InvoicePaymentInput(accountId, "BRL",
                day(28), "pay-1");
        service.payInvoice(ANA, invoiceId, payment);

        // A retried request (same key) is a no-op; a genuinely new payment attempt is refused.
        service.payInvoice(ANA, invoiceId, payment);
        assertMoney("1600.00", balance(ANA, accountId));

        assertEquals("INVOICE_ALREADY_PAID", code(assertThrows(HealthConflictException.class,
                () -> service.payInvoice(ANA, invoiceId, new HealthService.InvoicePaymentInput(accountId,
                        "BRL", day(28), "pay-2")))));
        assertMoney("1600.00", balance(ANA, accountId));
    }

    @Test
    void anInvoiceWithNoInstallmentsOnItIsNotPayable() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long accountId = account(ANA, "Corrente", "2000.00", CurrencyCode.BRL);
        long cardId = card(ANA, 20, 28, CurrencyCode.BRL);
        // No card flow produces an empty invoice, so this one is written straight through the
        // store: paying it would move real money out of the account against nothing at all.
        Invoice empty = store.createInvoice(users.get(ANA).getId(), cardId, CurrencyCode.BRL, thisMonth(),
                day(20), day(28));

        assertEquals("EMPTY_INVOICE", code(assertThrows(HealthConflictException.class,
                () -> service.payInvoice(ANA, empty.id(), new HealthService.InvoicePaymentInput(accountId,
                        "BRL", day(28), "pay-empty")))));
        assertMoney("2000.00", balance(ANA, accountId));
    }

    @Test
    void cardSpendIsRecognisedByItsInstallmentsInTheMonthTheyFallDue() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        account(ANA, "Corrente", "5000.00", CurrencyCode.BRL);
        long cardId = card(ANA, 20, 28, CurrencyCode.BRL);
        service.createPurchase(ANA, cardId, new HealthService.PurchaseInput("300.00", "BRL", "Curso",
                "education", thisMonth().atDay(1), 3, "buy-course"));

        assertMoney("100.00", service.summary(ANA, thisMonth()).realizedExpenses());
        assertMoney("100.00", service.summary(ANA, thisMonth().plusMonths(1)).realizedExpenses());
        assertMoney("100.00", service.summary(ANA, thisMonth().plusMonths(2)).realizedExpenses());
        assertMoney("0.00", service.summary(ANA, thisMonth().plusMonths(3)).realizedExpenses());
    }

    // ------------------------------------------- summary: three distinct numbers

    @Test
    void currentResultAndProjectedBalanceAreThreeDifferentNumbers() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);
        entry(ANA, id, "INCOME", "REALIZED", "3000.00", CurrencyCode.BRL, "salary", day(5), "salary");
        entry(ANA, id, "EXPENSE", "REALIZED", "800.00", CurrencyCode.BRL, "housing", day(6), "rent");
        entry(ANA, id, "EXPENSE", "PLANNED", "200.00", CurrencyCode.BRL, "utilities", day(25), "power");
        entry(ANA, id, "INCOME", "PLANNED", "500.00", CurrencyCode.BRL, "freelance", day(28), "extra");

        MonthlySummary summary = service.summary(ANA, thisMonth());

        assertMoney("3200.00", summary.currentBalance());   // realised cash only
        assertMoney("2200.00", summary.monthResult());      // 3000 in, 800 out
        assertMoney("3500.00", summary.projectedEndBalance()); // 3200 + 500 planned in - 200 planned out
        assertMoney("500.00", summary.plannedIncome());
        assertMoney("200.00", summary.plannedExpenses());
        assertNotEquals(summary.currentBalance(), summary.projectedEndBalance());
    }

    @Test
    void theProjectionSubtractsAnOpenInvoiceDueThisMonthExactlyOnce() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);
        // Closing on the 1st and due on the 15th keeps this month's invoice open and due this month.
        long cardId = card(ANA, 1, 15, CurrencyCode.BRL);
        service.createPurchase(ANA, cardId, new HealthService.PurchaseInput("250.00", "BRL", "Farmácia",
                "health", thisMonth().atDay(1), 1, "buy-pharma"));

        MonthlySummary summary = service.summary(ANA, thisMonth());

        assertMoney("1000.00", summary.currentBalance());
        assertMoney("250.00", summary.openCardInvoices());
        assertMoney("750.00", summary.projectedEndBalance());
        assertMoney("250.00", summary.realizedExpenses());
        assertTrue(summary.upcoming().stream().anyMatch(u -> u.kind().equals("CARD_INVOICE")));
        assertEquals(0, balance(ANA, id).compareTo(new BigDecimal("1000.00")),
                "an unpaid invoice never touches the account balance");
    }

    @Test
    void expensesByCategoryCoverBothAccountEntriesAndCardInstallments() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "2000.00", CurrencyCode.BRL);
        long cardId = card(ANA, 20, 28, CurrencyCode.BRL);
        entry(ANA, id, "EXPENSE", "REALIZED", "120.00", CurrencyCode.BRL, "food", day(2), "market");
        service.createPurchase(ANA, cardId, new HealthService.PurchaseInput("80.00", "BRL", "Restaurante",
                "food", thisMonth().atDay(3), 1, "buy-rest"));

        MonthlySummary summary = service.summary(ANA, thisMonth());

        CategoryAmount food = summary.expensesByCategory().stream()
                .filter(c -> c.category().equals("food")).findFirst().orElseThrow();
        assertMoney("200.00", food.amount());
    }

    // -------------------------------------------------- currency: BR/PT and locks

    @Test
    void aPortugueseProfileRunsTheWholeFlowInEuros() {
        onboard(BRUNO, CountryCode.PT, CurrencyCode.EUR, "pt-PT");
        long accountId = account(BRUNO, "Conta à ordem", "850.00", CurrencyCode.EUR);
        long cardId = card(BRUNO, 25, 5, CurrencyCode.EUR);
        entry(BRUNO, accountId, "INCOME", "REALIZED", "1500.00", CurrencyCode.EUR, "salary", day(2), "pt-salary");
        entry(BRUNO, accountId, "EXPENSE", "REALIZED", "42.90", CurrencyCode.EUR, "utilities", day(12), "pt-power");
        service.createPurchase(BRUNO, cardId, new HealthService.PurchaseInput("999.99", "EUR", "Computador",
                "shopping", thisMonth().atDay(1), 3, "pt-buy"));

        MonthlySummary summary = service.summary(BRUNO, thisMonth());

        assertEquals(CurrencyCode.EUR, summary.currency());
        assertMoney("2307.10", summary.currentBalance());
        assertMoney("1500.00", summary.realizedIncome());
        assertMoney("376.23", summary.realizedExpenses()); // 42.90 + 333.33 of the first installment
        assertEquals("pt-PT", service.getProfile(BRUNO).orElseThrow().profile().localeTag());
        assertEquals(CountryCode.PT, service.getProfile(BRUNO).orElseThrow().profile().countryCode());
    }

    @Test
    void aBrazilianProfileRunsTheWholeFlowInReais() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long accountId = account(ANA, "Conta corrente", "850.00", CurrencyCode.BRL);
        long cardId = card(ANA, 25, 5, CurrencyCode.BRL);
        entry(ANA, accountId, "INCOME", "REALIZED", "1500.00", CurrencyCode.BRL, "salary", day(2), "br-salary");
        entry(ANA, accountId, "EXPENSE", "REALIZED", "42.90", CurrencyCode.BRL, "utilities", day(12), "br-power");
        service.createPurchase(ANA, cardId, new HealthService.PurchaseInput("999.99", "BRL", "Computador",
                "shopping", thisMonth().atDay(1), 3, "br-buy"));

        MonthlySummary summary = service.summary(ANA, thisMonth());

        assertEquals(CurrencyCode.BRL, summary.currency());
        assertMoney("2307.10", summary.currentBalance());
        assertMoney("376.23", summary.realizedExpenses());
        assertEquals("pt-BR", service.getProfile(ANA).orElseThrow().profile().localeTag());
    }

    @Test
    void aValueInTheWrongCurrencyIsRefusedInsteadOfBeingReinterpreted() {
        onboard(BRUNO, CountryCode.PT, CurrencyCode.EUR, "pt-PT");
        long accountId = account(BRUNO, "Conta à ordem", "100.00", CurrencyCode.EUR);

        HealthConflictException e = assertThrows(HealthConflictException.class,
                () -> entry(BRUNO, accountId, "EXPENSE", "REALIZED", "10.00", CurrencyCode.BRL, "food",
                        day(3), "wrong-currency"));

        assertEquals("CURRENCY_MISMATCH", code(e));
        assertTrue(service.listTransactions(BRUNO, null).isEmpty());
        assertMoney("100.00", balance(BRUNO, accountId));
    }

    @Test
    void countryCurrencyAndLanguageAreIndependentChoices() {
        // Someone in Portugal being paid in reais, reading the interface in pt-BR: allowed, because
        // these are three separate preferences and none of them is inferred from the others.
        onboard(BRUNO, CountryCode.PT, CurrencyCode.BRL, "pt-BR");

        Profile profile = service.getProfile(BRUNO).orElseThrow().profile();
        assertEquals(CountryCode.PT, profile.countryCode());
        assertEquals(CurrencyCode.BRL, profile.primaryCurrency());
        assertEquals("pt-BR", profile.localeTag());
    }

    @Test
    void theCurrencyCanStillBeCorrectedWhileTheProfileHasNoFinancialData() {
        onboard(BRUNO, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        assertTrue(service.getProfile(BRUNO).orElseThrow().currencyChangeAllowed());

        onboard(BRUNO, CountryCode.PT, CurrencyCode.EUR, "pt-PT");

        assertEquals(CurrencyCode.EUR, service.getProfile(BRUNO).orElseThrow().profile().primaryCurrency());
    }

    @Test
    void theCurrencyLocksAsSoonAsThereIsMoneyToReinterpret() {
        onboard(BRUNO, CountryCode.PT, CurrencyCode.EUR, "pt-PT");
        account(BRUNO, "Conta à ordem", "850.00", CurrencyCode.EUR);

        assertFalse(service.getProfile(BRUNO).orElseThrow().currencyChangeAllowed());
        HealthConflictException e = assertThrows(HealthConflictException.class,
                () -> onboard(BRUNO, CountryCode.BR, CurrencyCode.BRL, "pt-BR"));

        assertEquals("CURRENCY_CHANGE_LOCKED", code(e));
        // Nothing was partially applied: 850 euros did not silently become 850 reais.
        Profile profile = service.getProfile(BRUNO).orElseThrow().profile();
        assertEquals(CurrencyCode.EUR, profile.primaryCurrency());
        assertEquals(CountryCode.PT, profile.countryCode());
        assertEquals("pt-PT", profile.localeTag());
    }

    @Test
    void theInterfaceLanguageStillChangesAfterTheCurrencyIsLocked() {
        onboard(BRUNO, CountryCode.PT, CurrencyCode.EUR, "pt-PT");
        account(BRUNO, "Conta à ordem", "850.00", CurrencyCode.EUR);

        service.saveProfile(BRUNO, new HealthService.ProfileInput("PT", "EUR", "pt-BR"));

        assertEquals("pt-BR", service.getProfile(BRUNO).orElseThrow().profile().localeTag());
    }

    @Test
    void anUnsupportedCountryCurrencyOrLocaleIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.saveProfile(ANA, new HealthService.ProfileInput("US", "BRL", "pt-BR")));
        assertThrows(IllegalArgumentException.class,
                () -> service.saveProfile(ANA, new HealthService.ProfileInput("BR", "USD", "pt-BR")));
        assertThrows(IllegalArgumentException.class,
                () -> service.saveProfile(ANA, new HealthService.ProfileInput("BR", "BRL", "en-US")));
        assertTrue(service.getProfile(ANA).isEmpty());
    }

    // ----------------------------------------------------------- input guards

    @Test
    void moneyWithMoreThanTwoDecimalPlacesIsRejectedRatherThanRounded() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "100.00", CurrencyCode.BRL);

        assertThrows(IllegalArgumentException.class,
                () -> entry(ANA, id, "EXPENSE", "REALIZED", "10.005", CurrencyCode.BRL, "food", day(3), "k1"));
        assertThrows(IllegalArgumentException.class,
                () -> entry(ANA, id, "EXPENSE", "REALIZED", "0.00", CurrencyCode.BRL, "food", day(3), "k2"));
        assertThrows(IllegalArgumentException.class,
                () -> entry(ANA, id, "EXPENSE", "REALIZED", "-5.00", CurrencyCode.BRL, "food", day(3), "k3"));
        assertThrows(IllegalArgumentException.class,
                () -> entry(ANA, id, "EXPENSE", "REALIZED", "abc", CurrencyCode.BRL, "food", day(3), "k4"));
        assertTrue(service.listTransactions(ANA, null).isEmpty());
    }

    @Test
    void aTrailingZeroDoesNotMakeAValueTooPrecise() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "100.00", CurrencyCode.BRL);

        Transaction transaction = entry(ANA, id, "EXPENSE", "REALIZED", "10.500", CurrencyCode.BRL,
                "food", day(3), "trailing");

        assertMoney("10.50", transaction.amount());
    }

    @Test
    void aTransferOrInvoicePaymentTypeCannotBeCreatedThroughTheEntryEndpoint() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "100.00", CurrencyCode.BRL);

        assertThrows(IllegalArgumentException.class,
                () -> entry(ANA, id, "TRANSFER_IN", "REALIZED", "10.00", CurrencyCode.BRL, "x", day(3), "t1"));
        assertThrows(IllegalArgumentException.class,
                () -> entry(ANA, id, "INVOICE_PAYMENT", "REALIZED", "10.00", CurrencyCode.BRL, "x", day(3), "t2"));
    }

    @Test
    void nothingCanBeCreatedBeforeOnboardingIsComplete() {
        assertThrows(ResourceNotFoundException.class, () -> service.listAccounts(ANA));
        assertThrows(ResourceNotFoundException.class, () -> service.createAccount(ANA,
                new HealthService.AccountInput("Corrente", "CHECKING", "10.00", LocalDate.now(), "BRL", "k")));
        assertThrows(ResourceNotFoundException.class, () -> service.summary(ANA, thisMonth()));
        assertTrue(service.getProfile(ANA).isEmpty());
    }

    @Test
    void aSoftDeletedEntryIsNotResurrectedByItsOldIdempotencyKey() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "100.00", CurrencyCode.BRL);
        Transaction transaction = entry(ANA, id, "EXPENSE", "REALIZED", "10.00", CurrencyCode.BRL,
                "food", day(3), "reused-after-delete");
        service.deleteTransaction(ANA, transaction.id());

        // Same key, same data: the row is gone from every listing, so re-sending it must not be
        // silently answered with the deleted row as if the expense were still there.
        HealthConflictException e = assertThrows(HealthConflictException.class,
                () -> entry(ANA, id, "EXPENSE", "REALIZED", "10.00", CurrencyCode.BRL, "food", day(3),
                        "reused-after-delete"));

        assertEquals("IDEMPOTENCY_KEY_REUSED", code(e));
        assertTrue(service.listTransactions(ANA, null).isEmpty());
        assertMoney("100.00", balance(ANA, id));
    }

    @Test
    void filtersNarrowTheEntryListWithoutLeavingTheUsersOwnData() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long checking = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);
        long wallet = account(ANA, "Carteira", "100.00", CurrencyCode.BRL);
        entry(ANA, checking, "EXPENSE", "REALIZED", "10.00", CurrencyCode.BRL, "food", day(2), "f1");
        entry(ANA, wallet, "EXPENSE", "PLANNED", "20.00", CurrencyCode.BRL, "leisure", day(20), "f2");

        assertEquals(1, service.listTransactions(ANA,
                new HealthService.TransactionFilter(null, null, checking, null, null)).size());
        assertEquals(1, service.listTransactions(ANA,
                new HealthService.TransactionFilter(null, null, null, "FOOD", null)).size());
        assertEquals(1, service.listTransactions(ANA,
                new HealthService.TransactionFilter(null, null, null, null, "PLANNED")).size());
        assertEquals(2, service.listTransactions(ANA,
                new HealthService.TransactionFilter(thisMonth().atDay(1), thisMonth().atEndOfMonth(),
                        null, null, null)).size());
        assertTrue(service.listTransactions(ANA, new HealthService.TransactionFilter(
                thisMonth().minusMonths(3).atDay(1), thisMonth().minusMonths(3).atEndOfMonth(),
                null, null, null)).isEmpty());
    }

    @Test
    void theStoreNeverReturnsAnotherUsersRowEvenWhenAskedDirectlyById() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        onboard(BRUNO, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long anaCard = card(ANA, 20, 28, CurrencyCode.BRL);
        service.createPurchase(ANA, anaCard, new HealthService.PurchaseInput("100.00", "BRL", "Compra",
                "shopping", thisMonth().atDay(1), 1, "ana-buy"));
        long anaInvoice = service.listInvoices(ANA, anaCard).getFirst().invoice().id();
        long brunoAccount = account(BRUNO, "Corrente", "5000.00", CurrencyCode.BRL);

        assertThrows(ResourceNotFoundException.class, () -> service.listInvoices(BRUNO, anaCard));
        assertThrows(ResourceNotFoundException.class, () -> service.payInvoice(BRUNO, anaInvoice,
                new HealthService.InvoicePaymentInput(brunoAccount, "BRL", day(28), "steal")));
        assertEquals(InvoiceStatus.OPEN, service.listInvoices(ANA, anaCard).getFirst().invoice().status());
        assertMoney("5000.00", balance(BRUNO, brunoAccount));
    }

    @Test
    void anAccountBalanceIgnoresMovementsDatedBeforeItsReferenceDate() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        // The opening balance is stated as of the 1st of this month, so it already includes
        // whatever happened before that; counting an older expense again would double it.
        long id = account(ANA, "Corrente", "1000.00", CurrencyCode.BRL);
        entry(ANA, id, "EXPENSE", "REALIZED", "70.00", CurrencyCode.BRL, "food",
                thisMonth().minusMonths(1).atDay(15), "older");

        assertMoney("1000.00", balance(ANA, id));
    }

    @Test
    void theSameIdempotencyKeyBelongingToTwoUsersDoesNotCollide() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        onboard(BRUNO, CountryCode.BR, CurrencyCode.BRL, "pt-BR");

        HealthService.AccountView ana = service.createAccount(ANA, new HealthService.AccountInput("Corrente",
                "CHECKING", "10.00", thisMonth().atDay(1), "BRL", "shared-key"));
        HealthService.AccountView bruno = service.createAccount(BRUNO, new HealthService.AccountInput("Corrente",
                "CHECKING", "20.00", thisMonth().atDay(1), "BRL", "shared-key"));

        assertNotEquals(ana.account().id(), bruno.account().id());
        assertMoney("10.00", ana.currentBalance());
        assertMoney("20.00", bruno.currentBalance());
    }

    @Test
    void aHealthRowCarriesItsOriginSoAFutureImportCanBeToldApartFromManualEntry() {
        onboard(ANA, CountryCode.BR, CurrencyCode.BRL, "pt-BR");
        long id = account(ANA, "Corrente", "100.00", CurrencyCode.BRL);
        Transaction manual = entry(ANA, id, "EXPENSE", "REALIZED", "10.00", CurrencyCode.BRL, "food",
                day(3), "origin");

        assertSame(RecordSource.MANUAL, manual.source());
        assertNull(manual.externalProvider());
        assertNull(manual.externalId());
    }
}
