package com.jf.PetApp.infrastructure.repository.health;

import static com.jf.PetApp.core.domain.health.HealthModels.*;

import com.jf.PetApp.application.health.port.HealthStore;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Compact JDBC adapter for Health. Dev/H2 keeps migrations unqualified while
 * production moves the exact same tables to `health` (V30), so only the table
 * prefix differs; every query and ownership predicate is otherwise identical.
 */
@Repository
public class JdbcHealthStore implements HealthStore {

    private final JdbcTemplate jdbc;
    private final String prefix;

    public JdbcHealthStore(JdbcTemplate jdbc, Environment environment) {
        this.jdbc = jdbc;
        this.prefix = environment.acceptsProfiles(Profiles.of("prod")) ? "health." : "";
    }

    private String t(String name) {
        return prefix + name;
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static LocalDate date(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static YearMonth month(ResultSet rs, String column) throws SQLException {
        LocalDate value = date(rs, column);
        return value == null ? null : YearMonth.from(value);
    }

    private <T> Optional<T> first(List<T> values) {
        return values.stream().findFirst();
    }

    private Profile mapProfile(ResultSet rs, int row) throws SQLException {
        return new Profile(
                rs.getLong("user_id"),
                CountryCode.valueOf(rs.getString("country_code")),
                CurrencyCode.valueOf(rs.getString("primary_currency")),
                rs.getString("locale_tag"),
                instant(rs, "created_at"),
                instant(rs, "updated_at"));
    }

    private Account mapAccount(ResultSet rs, int row) throws SQLException {
        return new Account(
                rs.getLong("id"), rs.getLong("user_id"), rs.getString("name"),
                AccountType.valueOf(rs.getString("account_type")), rs.getBigDecimal("initial_balance"),
                date(rs, "balance_reference_date"), CurrencyCode.valueOf(rs.getString("currency")),
                rs.getBoolean("archived"), rs.getString("idempotency_key"),
                instant(rs, "created_at"), instant(rs, "updated_at"));
    }

    private Transaction mapTransaction(ResultSet rs, int row) throws SQLException {
        Long transferId = rs.getObject("transfer_id") == null ? null : rs.getLong("transfer_id");
        Long recurrenceId = rs.getObject("recurrence_id") == null ? null : rs.getLong("recurrence_id");
        Long invoiceId = rs.getObject("invoice_id") == null ? null : rs.getLong("invoice_id");
        return new Transaction(
                rs.getLong("id"), rs.getLong("user_id"), rs.getLong("account_id"),
                EntryType.valueOf(rs.getString("entry_type")), EntryStatus.valueOf(rs.getString("entry_status")),
                rs.getBigDecimal("amount"), CurrencyCode.valueOf(rs.getString("currency")),
                rs.getString("description"), rs.getString("category"), date(rs, "transaction_date"),
                RecordSource.valueOf(rs.getString("record_source")), rs.getString("external_provider"),
                rs.getString("external_id"), rs.getString("idempotency_key"), transferId, recurrenceId,
                month(rs, "recurrence_month"), invoiceId, instant(rs, "deleted_at"),
                instant(rs, "created_at"), instant(rs, "updated_at"));
    }

    private Transfer mapTransfer(ResultSet rs, int row) throws SQLException {
        return new Transfer(
                rs.getLong("id"), rs.getLong("user_id"), rs.getLong("from_account_id"),
                rs.getLong("to_account_id"), rs.getBigDecimal("amount"),
                CurrencyCode.valueOf(rs.getString("currency")), date(rs, "transfer_date"),
                rs.getString("description"), rs.getString("idempotency_key"), instant(rs, "created_at"));
    }

    private Recurrence mapRecurrence(ResultSet rs, int row) throws SQLException {
        return new Recurrence(
                rs.getLong("id"), rs.getLong("user_id"), rs.getLong("account_id"),
                EntryType.valueOf(rs.getString("entry_type")), rs.getBigDecimal("amount"),
                CurrencyCode.valueOf(rs.getString("currency")), rs.getString("description"),
                rs.getString("category"), rs.getInt("day_of_month"), date(rs, "start_date"),
                date(rs, "end_date"), rs.getBoolean("active"), rs.getString("idempotency_key"),
                instant(rs, "created_at"), instant(rs, "updated_at"));
    }

    private Card mapCard(ResultSet rs, int row) throws SQLException {
        return new Card(
                rs.getLong("id"), rs.getLong("user_id"), rs.getString("name"),
                CurrencyCode.valueOf(rs.getString("currency")), rs.getInt("closing_day"),
                rs.getInt("due_day"), rs.getBoolean("archived"), rs.getString("idempotency_key"),
                instant(rs, "created_at"), instant(rs, "updated_at"));
    }

    private Invoice mapInvoice(ResultSet rs, int row) throws SQLException {
        Long paidTransactionId = rs.getObject("paid_transaction_id") == null
                ? null : rs.getLong("paid_transaction_id");
        return new Invoice(
                rs.getLong("id"), rs.getLong("user_id"), rs.getLong("card_id"),
                CurrencyCode.valueOf(rs.getString("currency")), month(rs, "cycle_month"),
                date(rs, "closing_date"), date(rs, "due_date"),
                InvoiceStatus.valueOf(rs.getString("invoice_status")), date(rs, "paid_at"),
                paidTransactionId, instant(rs, "created_at"), instant(rs, "updated_at"));
    }

    private Purchase mapPurchase(ResultSet rs, int row) throws SQLException {
        return new Purchase(
                rs.getLong("id"), rs.getLong("user_id"), rs.getLong("card_id"),
                rs.getBigDecimal("total_amount"), CurrencyCode.valueOf(rs.getString("currency")),
                rs.getString("description"), rs.getString("category"), date(rs, "purchase_date"),
                rs.getInt("installment_count"), RecordSource.valueOf(rs.getString("record_source")),
                rs.getString("external_provider"), rs.getString("external_id"),
                rs.getString("idempotency_key"), instant(rs, "created_at"));
    }

    private Installment mapInstallment(ResultSet rs, int row) throws SQLException {
        return new Installment(
                rs.getLong("id"), rs.getLong("user_id"), rs.getLong("purchase_id"),
                rs.getLong("invoice_id"), CurrencyCode.valueOf(rs.getString("currency")),
                rs.getInt("installment_number"), rs.getInt("installment_count"),
                rs.getBigDecimal("amount"));
    }

    @Override
    public Optional<Profile> findProfile(long userId) {
        return first(jdbc.query("select * from " + t("health_profiles") + " where user_id = ?",
                this::mapProfile, userId));
    }

    @Override
    public Optional<Profile> findProfileForUpdate(long userId) {
        return first(jdbc.query("select * from " + t("health_profiles") + " where user_id = ? for update",
                this::mapProfile, userId));
    }

    @Override
    public Profile createProfile(long userId, CountryCode country, CurrencyCode currency, String localeTag) {
        Instant now = Instant.now();
        jdbc.update("insert into " + t("health_profiles")
                        + " (user_id,country_code,primary_currency,locale_tag,created_at,updated_at) values (?,?,?,?,?,?)",
                userId, country.name(), currency.name(), localeTag, Timestamp.from(now), Timestamp.from(now));
        return findProfile(userId).orElseThrow();
    }

    @Override
    public Profile updateProfile(long userId, CountryCode country, CurrencyCode currency, String localeTag) {
        jdbc.update("update " + t("health_profiles")
                        + " set country_code=?, primary_currency=?, locale_tag=?, updated_at=? where user_id=?",
                country.name(), currency.name(), localeTag, Timestamp.from(Instant.now()), userId);
        return findProfile(userId).orElseThrow();
    }

    @Override
    public boolean hasFinancialData(long userId) {
        String sql = "select case when "
                + "exists(select 1 from " + t("health_accounts") + " where user_id=?) or "
                + "exists(select 1 from " + t("health_cards") + " where user_id=?) or "
                + "exists(select 1 from " + t("health_recurrences") + " where user_id=?) or "
                + "exists(select 1 from " + t("health_transactions") + " where user_id=?) or "
                + "exists(select 1 from " + t("health_card_purchases") + " where user_id=?) "
                + "then 1 else 0 end";
        Integer found = jdbc.queryForObject(sql, Integer.class, userId, userId, userId, userId, userId);
        return found != null && found == 1;
    }

    @Override
    public List<Account> listAccounts(long userId) {
        return jdbc.query("select * from " + t("health_accounts")
                + " where user_id=? order by archived, created_at, id", this::mapAccount, userId);
    }

    @Override
    public Optional<Account> findAccount(long userId, long accountId) {
        return first(jdbc.query("select * from " + t("health_accounts") + " where user_id=? and id=?",
                this::mapAccount, userId, accountId));
    }

    @Override
    public Optional<Account> findAccountByIdempotencyKey(long userId, String key) {
        return first(jdbc.query("select * from " + t("health_accounts")
                + " where user_id=? and idempotency_key=?", this::mapAccount, userId, key));
    }

    @Override
    public Account createAccount(long userId, String name, AccountType type, BigDecimal initialBalance,
                                 LocalDate referenceDate, CurrencyCode currency, String key) {
        Instant now = Instant.now();
        jdbc.update("insert into " + t("health_accounts") + " (user_id,name,account_type,initial_balance,"
                        + "balance_reference_date,currency,archived,idempotency_key,created_at,updated_at) "
                        + "values (?,?,?,?,?,?,false,?,?,?)",
                userId, name, type.name(), initialBalance, referenceDate, currency.name(), key,
                Timestamp.from(now), Timestamp.from(now));
        return findAccountByIdempotencyKey(userId, key).orElseThrow();
    }

    @Override
    public Account updateAccount(long userId, long accountId, String name, AccountType type,
                                 BigDecimal initialBalance, LocalDate referenceDate, CurrencyCode currency) {
        jdbc.update("update " + t("health_accounts") + " set name=?,account_type=?,initial_balance=?,"
                        + "balance_reference_date=?,currency=?,updated_at=? where user_id=? and id=?",
                name, type.name(), initialBalance, referenceDate, currency.name(), Timestamp.from(Instant.now()),
                userId, accountId);
        return findAccount(userId, accountId).orElseThrow();
    }

    @Override
    public void archiveAccount(long userId, long accountId) {
        jdbc.update("update " + t("health_accounts") + " set archived=true,updated_at=? where user_id=? and id=?",
                Timestamp.from(Instant.now()), userId, accountId);
    }

    @Override
    public BigDecimal accountBalance(long userId, Account account) {
        String sql = "select coalesce(sum(case "
                + "when entry_type in ('INCOME','TRANSFER_IN') then amount "
                + "when entry_type in ('EXPENSE','TRANSFER_OUT','INVOICE_PAYMENT') then -amount else 0 end),0) "
                + "from " + t("health_transactions")
                + " where user_id=? and account_id=? and entry_status='REALIZED' and deleted_at is null "
                + "and transaction_date>=?";
        BigDecimal movement = jdbc.queryForObject(sql, BigDecimal.class,
                userId, account.id(), account.balanceReferenceDate());
        return account.initialBalance().add(movement == null ? BigDecimal.ZERO : movement);
    }

    @Override
    public List<Transaction> listTransactions(long userId) {
        return jdbc.query("select * from " + t("health_transactions")
                + " where user_id=? and deleted_at is null order by transaction_date desc,id desc",
                this::mapTransaction, userId);
    }

    @Override
    public Optional<Transaction> findTransaction(long userId, long transactionId) {
        return first(jdbc.query("select * from " + t("health_transactions")
                + " where user_id=? and id=? and deleted_at is null", this::mapTransaction, userId, transactionId));
    }

    @Override
    public Optional<Transaction> findTransactionByIdempotencyKey(long userId, String key) {
        return first(jdbc.query("select * from " + t("health_transactions")
                + " where user_id=? and idempotency_key=?", this::mapTransaction, userId, key));
    }

    @Override
    public List<Transaction> findTransactionsByTransfer(long userId, long transferId) {
        return jdbc.query("select * from " + t("health_transactions")
                + " where user_id=? and transfer_id=? and deleted_at is null order by id",
                this::mapTransaction, userId, transferId);
    }

    @Override
    public Transaction createTransaction(long userId, long accountId, EntryType type, EntryStatus status,
                                         BigDecimal amount, CurrencyCode currency, String description,
                                         String category, LocalDate transactionDate, RecordSource source,
                                         String externalProvider, String externalId, String key,
                                         Long transferId, Long recurrenceId, YearMonth recurrenceMonth,
                                         Long invoiceId) {
        Instant now = Instant.now();
        jdbc.update("insert into " + t("health_transactions") + " (user_id,account_id,entry_type,entry_status,"
                        + "amount,currency,description,category,transaction_date,record_source,external_provider,"
                        + "external_id,idempotency_key,transfer_id,recurrence_id,recurrence_month,invoice_id,"
                        + "created_at,updated_at) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                userId, accountId, type.name(), status.name(), amount, currency.name(), description, category,
                transactionDate, source.name(), externalProvider, externalId, key, transferId, recurrenceId,
                recurrenceMonth == null ? null : recurrenceMonth.atDay(1), invoiceId,
                Timestamp.from(now), Timestamp.from(now));
        return findTransactionByIdempotencyKey(userId, key).orElseThrow();
    }

    @Override
    public Transaction updateTransaction(long userId, long transactionId, long accountId, EntryType type,
                                         EntryStatus status, BigDecimal amount, CurrencyCode currency,
                                         String description, String category, LocalDate transactionDate) {
        jdbc.update("update " + t("health_transactions") + " set account_id=?,entry_type=?,entry_status=?,"
                        + "amount=?,currency=?,description=?,category=?,transaction_date=?,updated_at=? "
                        + "where user_id=? and id=? and deleted_at is null",
                accountId, type.name(), status.name(), amount, currency.name(), description, category,
                transactionDate, Timestamp.from(Instant.now()), userId, transactionId);
        return findTransaction(userId, transactionId).orElseThrow();
    }

    @Override
    public void softDeleteTransaction(long userId, long transactionId) {
        jdbc.update("update " + t("health_transactions") + " set deleted_at=?,updated_at=? "
                        + "where user_id=? and id=? and deleted_at is null",
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now()), userId, transactionId);
    }

    @Override
    public void deletePlannedRecurrenceOccurrencesFrom(long userId, long recurrenceId, LocalDate from) {
        jdbc.update("update " + t("health_transactions") + " set deleted_at=?,updated_at=? where user_id=? "
                        + "and recurrence_id=? and entry_status='PLANNED' and transaction_date>=? and deleted_at is null",
                Timestamp.from(Instant.now()), Timestamp.from(Instant.now()), userId, recurrenceId, from);
    }

    @Override
    public Optional<Transfer> findTransferByIdempotencyKey(long userId, String key) {
        return first(jdbc.query("select * from " + t("health_transfers")
                + " where user_id=? and idempotency_key=?", this::mapTransfer, userId, key));
    }

    @Override
    public Transfer createTransfer(long userId, long fromAccountId, long toAccountId, BigDecimal amount,
                                   CurrencyCode currency, LocalDate transferDate, String description, String key) {
        jdbc.update("insert into " + t("health_transfers")
                        + " (user_id,from_account_id,to_account_id,amount,currency,transfer_date,description,"
                        + "idempotency_key,created_at) values (?,?,?,?,?,?,?,?,?)",
                userId, fromAccountId, toAccountId, amount, currency.name(), transferDate, description, key,
                Timestamp.from(Instant.now()));
        return findTransferByIdempotencyKey(userId, key).orElseThrow();
    }

    @Override
    public List<Recurrence> listRecurrences(long userId) {
        return jdbc.query("select * from " + t("health_recurrences")
                + " where user_id=? order by active desc,created_at,id", this::mapRecurrence, userId);
    }

    @Override
    public Optional<Recurrence> findRecurrence(long userId, long recurrenceId) {
        return first(jdbc.query("select * from " + t("health_recurrences")
                + " where user_id=? and id=?", this::mapRecurrence, userId, recurrenceId));
    }

    @Override
    public Optional<Recurrence> findRecurrenceByIdempotencyKey(long userId, String key) {
        return first(jdbc.query("select * from " + t("health_recurrences")
                + " where user_id=? and idempotency_key=?", this::mapRecurrence, userId, key));
    }

    @Override
    public Recurrence createRecurrence(long userId, long accountId, EntryType type, BigDecimal amount,
                                       CurrencyCode currency, String description, String category, int dayOfMonth,
                                       LocalDate startDate, LocalDate endDate, String key) {
        Instant now = Instant.now();
        jdbc.update("insert into " + t("health_recurrences") + " (user_id,account_id,entry_type,amount,currency,"
                        + "description,category,day_of_month,start_date,end_date,active,idempotency_key,created_at,"
                        + "updated_at) values (?,?,?,?,?,?,?,?,?,?,true,?,?,?)",
                userId, accountId, type.name(), amount, currency.name(), description, category, dayOfMonth,
                startDate, endDate, key, Timestamp.from(now), Timestamp.from(now));
        return findRecurrenceByIdempotencyKey(userId, key).orElseThrow();
    }

    @Override
    public Recurrence updateRecurrence(long userId, long recurrenceId, long accountId, EntryType type,
                                       BigDecimal amount, CurrencyCode currency, String description,
                                       String category, int dayOfMonth, LocalDate startDate, LocalDate endDate) {
        jdbc.update("update " + t("health_recurrences") + " set account_id=?,entry_type=?,amount=?,currency=?,"
                        + "description=?,category=?,day_of_month=?,start_date=?,end_date=?,updated_at=? "
                        + "where user_id=? and id=?",
                accountId, type.name(), amount, currency.name(), description, category, dayOfMonth,
                startDate, endDate, Timestamp.from(Instant.now()), userId, recurrenceId);
        return findRecurrence(userId, recurrenceId).orElseThrow();
    }

    @Override
    public void deactivateRecurrence(long userId, long recurrenceId) {
        jdbc.update("update " + t("health_recurrences") + " set active=false,updated_at=? where user_id=? and id=?",
                Timestamp.from(Instant.now()), userId, recurrenceId);
    }

    @Override
    public List<Card> listCards(long userId) {
        return jdbc.query("select * from " + t("health_cards")
                + " where user_id=? order by archived,created_at,id", this::mapCard, userId);
    }

    @Override
    public Optional<Card> findCard(long userId, long cardId) {
        return first(jdbc.query("select * from " + t("health_cards") + " where user_id=? and id=?",
                this::mapCard, userId, cardId));
    }

    @Override
    public Optional<Card> findCardByIdempotencyKey(long userId, String key) {
        return first(jdbc.query("select * from " + t("health_cards")
                + " where user_id=? and idempotency_key=?", this::mapCard, userId, key));
    }

    @Override
    public Card createCard(long userId, String name, CurrencyCode currency, int closingDay, int dueDay, String key) {
        Instant now = Instant.now();
        jdbc.update("insert into " + t("health_cards") + " (user_id,name,currency,closing_day,due_day,archived,"
                        + "idempotency_key,created_at,updated_at) values (?,?,?,?,?,false,?,?,?)",
                userId, name, currency.name(), closingDay, dueDay, key, Timestamp.from(now), Timestamp.from(now));
        return findCardByIdempotencyKey(userId, key).orElseThrow();
    }

    @Override
    public Card updateCard(long userId, long cardId, String name, CurrencyCode currency, int closingDay, int dueDay) {
        jdbc.update("update " + t("health_cards")
                        + " set name=?,currency=?,closing_day=?,due_day=?,updated_at=? where user_id=? and id=?",
                name, currency.name(), closingDay, dueDay, Timestamp.from(Instant.now()), userId, cardId);
        return findCard(userId, cardId).orElseThrow();
    }

    @Override
    public void archiveCard(long userId, long cardId) {
        jdbc.update("update " + t("health_cards") + " set archived=true,updated_at=? where user_id=? and id=?",
                Timestamp.from(Instant.now()), userId, cardId);
    }

    @Override
    public Optional<Invoice> findInvoice(long userId, long invoiceId) {
        return first(jdbc.query("select * from " + t("health_card_invoices") + " where user_id=? and id=?",
                this::mapInvoice, userId, invoiceId));
    }

    @Override
    public Optional<Invoice> findInvoiceByCardAndCycle(long userId, long cardId, YearMonth cycleMonth) {
        return first(jdbc.query("select * from " + t("health_card_invoices")
                        + " where user_id=? and card_id=? and cycle_month=?",
                this::mapInvoice, userId, cardId, cycleMonth.atDay(1)));
    }

    @Override
    public Invoice createInvoice(long userId, long cardId, CurrencyCode currency, YearMonth cycleMonth,
                                 LocalDate closingDate, LocalDate dueDate) {
        Instant now = Instant.now();
        jdbc.update("insert into " + t("health_card_invoices") + " (user_id,card_id,currency,cycle_month,"
                        + "closing_date,due_date,invoice_status,created_at,updated_at) values (?,?,?,?,?,?,'OPEN',?,?)",
                userId, cardId, currency.name(), cycleMonth.atDay(1), closingDate, dueDate,
                Timestamp.from(now), Timestamp.from(now));
        return findInvoiceByCardAndCycle(userId, cardId, cycleMonth).orElseThrow();
    }

    @Override
    public List<Invoice> listInvoices(long userId, long cardId) {
        return jdbc.query("select * from " + t("health_card_invoices")
                + " where user_id=? and card_id=? order by cycle_month desc", this::mapInvoice, userId, cardId);
    }

    @Override
    public List<Invoice> listInvoices(long userId) {
        return jdbc.query("select * from " + t("health_card_invoices")
                + " where user_id=? order by due_date,id", this::mapInvoice, userId);
    }

    @Override
    public BigDecimal invoiceTotal(long userId, long invoiceId) {
        BigDecimal total = jdbc.queryForObject("select coalesce(sum(amount),0) from "
                        + t("health_card_installments") + " where user_id=? and invoice_id=?",
                BigDecimal.class, userId, invoiceId);
        return total == null ? BigDecimal.ZERO : total;
    }

    @Override
    public Invoice markInvoicePaid(long userId, long invoiceId, LocalDate paidAt, long transactionId) {
        jdbc.update("update " + t("health_card_invoices") + " set invoice_status='PAID',paid_at=?,"
                        + "paid_transaction_id=?,updated_at=? where user_id=? and id=? and invoice_status='OPEN'",
                paidAt, transactionId, Timestamp.from(Instant.now()), userId, invoiceId);
        return findInvoice(userId, invoiceId).orElseThrow();
    }

    @Override
    public Optional<Purchase> findPurchaseByIdempotencyKey(long userId, String key) {
        return first(jdbc.query("select * from " + t("health_card_purchases")
                + " where user_id=? and idempotency_key=?", this::mapPurchase, userId, key));
    }

    @Override
    public Purchase createPurchase(long userId, long cardId, BigDecimal totalAmount, CurrencyCode currency,
                                   String description, String category, LocalDate purchaseDate,
                                   int installmentCount, RecordSource source, String externalProvider,
                                   String externalId, String key) {
        jdbc.update("insert into " + t("health_card_purchases") + " (user_id,card_id,total_amount,currency,"
                        + "description,category,purchase_date,installment_count,record_source,external_provider,"
                        + "external_id,idempotency_key,created_at) values (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                userId, cardId, totalAmount, currency.name(), description, category, purchaseDate,
                installmentCount, source.name(), externalProvider, externalId, key, Timestamp.from(Instant.now()));
        return findPurchaseByIdempotencyKey(userId, key).orElseThrow();
    }

    @Override
    public Installment createInstallment(long userId, long purchaseId, long invoiceId,
                                         CurrencyCode currency, int number, int count, BigDecimal amount) {
        jdbc.update("insert into " + t("health_card_installments")
                        + " (user_id,purchase_id,invoice_id,currency,installment_number,installment_count,amount) "
                        + "values (?,?,?,?,?,?,?)",
                userId, purchaseId, invoiceId, currency.name(), number, count, amount);
        return listInstallmentsByPurchase(userId, purchaseId).stream()
                .filter(i -> i.installmentNumber() == number).findFirst().orElseThrow();
    }

    @Override
    public List<Installment> listInstallmentsByPurchase(long userId, long purchaseId) {
        return jdbc.query("select * from " + t("health_card_installments")
                        + " where user_id=? and purchase_id=? order by installment_number",
                this::mapInstallment, userId, purchaseId);
    }

    @Override
    public List<Installment> listInstallmentsByInvoice(long userId, long invoiceId) {
        return jdbc.query("select * from " + t("health_card_installments")
                        + " where user_id=? and invoice_id=? order by id",
                this::mapInstallment, userId, invoiceId);
    }

    @Override
    public List<Purchase> listPurchases(long userId) {
        return jdbc.query("select * from " + t("health_card_purchases")
                + " where user_id=? order by purchase_date,id", this::mapPurchase, userId);
    }
}
