package db.migration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * The guarantees V29 puts in the database itself, rather than in {@code HealthService}. They are
 * the last line of defence for the invariants the whole product rests on — one currency per
 * profile, one occurrence per recurring month, one payment per invoice — and they must survive a
 * bug in the service, a future import job, or a hand-written fix applied straight to the database.
 *
 * <p>Runs the real migrations against a scratch H2 database, the same way
 * {@link FlywayMigrationTest} does; the rest of the suite keeps Flyway disabled by design (see
 * {@code src/test/resources/application.properties}).
 */
class HealthSchemaConstraintsTest {

    private SingleConnectionDataSource dataSource;
    private Statement statement;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = new SingleConnectionDataSource(
                "jdbc:h2:mem:health-schema-" + System.nanoTime(), "sa", "", true);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        statement = dataSource.getConnection().createStatement();

        statement.execute("insert into jf_users (email, password, is_active) values ('ana@test.com','x',true)");
        statement.execute("insert into health_profiles (user_id, country_code, primary_currency, locale_tag,"
                + " created_at, updated_at) select user_id,'PT','EUR','pt-PT',current_timestamp,"
                + "current_timestamp from jf_users where email='ana@test.com'");
        statement.execute("insert into health_accounts (user_id,name,account_type,initial_balance,"
                + "balance_reference_date,currency,idempotency_key,created_at,updated_at) "
                + "select user_id,'Conta','CHECKING',100.00,date '2026-09-01','EUR','acc-1',"
                + "current_timestamp,current_timestamp from jf_users where email='ana@test.com'");
    }

    @AfterEach
    void tearDown() {
        dataSource.destroy();
    }

    private long userId() throws SQLException {
        try (var rs = statement.executeQuery("select user_id from jf_users where email='ana@test.com'")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private long accountId() throws SQLException {
        try (var rs = statement.executeQuery("select id from health_accounts where idempotency_key='acc-1'")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private void insertTransaction(String currency, String key, String recurrenceMonth) throws SQLException {
        statement.execute("insert into health_transactions (user_id,account_id,entry_type,entry_status,amount,"
                + "currency,description,transaction_date,record_source,idempotency_key,recurrence_month,"
                + "created_at,updated_at) values (" + userId() + "," + accountId() + ",'EXPENSE','REALIZED',"
                + "10.00,'" + currency + "','Compra',date '2026-09-10','MANUAL','" + key + "',"
                + (recurrenceMonth == null ? "null" : "date '" + recurrenceMonth + "'")
                + ",current_timestamp,current_timestamp)");
    }

    @Test
    void anAccountCannotUseACurrencyOtherThanTheProfilesPrimaryOne() {
        // The composite foreign key to (user_id, primary_currency) is what makes "one currency per
        // user" a database fact rather than a convention the service happens to follow.
        assertThrows(SQLException.class, () -> statement.execute(
                "insert into health_accounts (user_id,name,account_type,initial_balance,"
                        + "balance_reference_date,currency,idempotency_key,created_at,updated_at) values ("
                        + userId() + ",'Conta BRL','CHECKING',100.00,date '2026-09-01','BRL','acc-2',"
                        + "current_timestamp,current_timestamp)"));
    }

    @Test
    void anEntryCannotCarryADifferentCurrencyFromItsAccount() {
        assertThrows(SQLException.class, () -> insertTransaction("BRL", "tx-mismatch", null));
    }

    @Test
    void theSameRecurringMonthCannotBeGeneratedTwice() throws SQLException {
        statement.execute("insert into health_recurrences (user_id,account_id,entry_type,amount,currency,"
                + "description,day_of_month,start_date,idempotency_key,created_at,updated_at) values ("
                + userId() + "," + accountId() + ",'EXPENSE',50.00,'EUR','Renda',10,date '2026-01-01',"
                + "'rec-1',current_timestamp,current_timestamp)");
        long recurrenceId;
        try (var rs = statement.executeQuery("select id from health_recurrences where idempotency_key='rec-1'")) {
            rs.next();
            recurrenceId = rs.getLong(1);
        }

        String occurrence = "insert into health_transactions (user_id,account_id,entry_type,entry_status,amount,"
                + "currency,description,transaction_date,record_source,idempotency_key,recurrence_id,"
                + "recurrence_month,created_at,updated_at) values (" + userId() + "," + accountId()
                + ",'EXPENSE','PLANNED',50.00,'EUR','Renda',date '2026-09-10','SYSTEM','%s',"
                + recurrenceId + ",date '2026-09-01',current_timestamp,current_timestamp)";

        assertDoesNotThrow(() -> statement.execute(String.format(occurrence, "occ-1")));
        assertThrows(SQLException.class, () -> statement.execute(String.format(occurrence, "occ-2")),
                "one recurrence produces at most one occurrence per month");
    }

    @Test
    void theSameIdempotencyKeyCannotBeUsedTwiceByOneUser() throws SQLException {
        insertTransaction("EUR", "duplicate-key", null);

        assertThrows(SQLException.class, () -> insertTransaction("EUR", "duplicate-key", null));
    }

    @Test
    void anInvoiceCannotHaveTwoPaymentTransactions() throws SQLException {
        statement.execute("insert into health_cards (user_id,name,currency,closing_day,due_day,"
                + "idempotency_key,created_at,updated_at) values (" + userId()
                + ",'Cartão','EUR',20,28,'card-1',current_timestamp,current_timestamp)");
        long cardId;
        try (var rs = statement.executeQuery("select id from health_cards where idempotency_key='card-1'")) {
            rs.next();
            cardId = rs.getLong(1);
        }
        statement.execute("insert into health_card_invoices (user_id,card_id,currency,cycle_month,closing_date,"
                + "due_date,invoice_status,created_at,updated_at) values (" + userId() + "," + cardId
                + ",'EUR',date '2026-09-01',date '2026-09-20',date '2026-09-28','OPEN',current_timestamp,"
                + "current_timestamp)");
        long invoiceId;
        try (var rs = statement.executeQuery("select id from health_card_invoices where card_id=" + cardId)) {
            rs.next();
            invoiceId = rs.getLong(1);
        }

        String payment = "insert into health_transactions (user_id,account_id,entry_type,entry_status,amount,"
                + "currency,description,transaction_date,record_source,idempotency_key,invoice_id,"
                + "created_at,updated_at) values (" + userId() + "," + accountId()
                + ",'INVOICE_PAYMENT','REALIZED',100.00,'EUR','Pagamento',date '2026-09-28','SYSTEM','%s',"
                + invoiceId + ",current_timestamp,current_timestamp)";

        assertDoesNotThrow(() -> statement.execute(String.format(payment, "pay-1")));
        assertThrows(SQLException.class, () -> statement.execute(String.format(payment, "pay-2")),
                "an invoice is payable exactly once");
    }

    @Test
    void aTransferCannotHaveTheSameAccountOnBothSides() throws SQLException {
        assertThrows(SQLException.class, () -> statement.execute(
                "insert into health_transfers (user_id,from_account_id,to_account_id,amount,currency,"
                        + "transfer_date,idempotency_key,created_at) values (" + userId() + "," + accountId()
                        + "," + accountId() + ",10.00,'EUR',date '2026-09-10','self',current_timestamp)"));
    }

    @Test
    void moneyColumnsRejectZeroAndNegativeAmounts() throws SQLException {
        String entry = "insert into health_transactions (user_id,account_id,entry_type,entry_status,amount,"
                + "currency,description,transaction_date,record_source,idempotency_key,created_at,updated_at) "
                + "values (" + userId() + "," + accountId() + ",'EXPENSE','REALIZED',%s,'EUR','x',"
                + "date '2026-09-10','MANUAL','%s',current_timestamp,current_timestamp)";

        assertThrows(SQLException.class, () -> statement.execute(String.format(entry, "0.00", "zero")));
        assertThrows(SQLException.class, () -> statement.execute(String.format(entry, "-5.00", "negative")));
    }

    @Test
    void anAmountIsStoredAtTwoDecimalPlacesAndNotAsABinaryFraction() throws SQLException {
        insertTransaction("EUR", "scale", null);
        statement.execute("update health_transactions set amount = 0.1 + 0.2 where idempotency_key='scale'");

        try (var rs = statement.executeQuery(
                "select amount from health_transactions where idempotency_key='scale'")) {
            rs.next();
            org.junit.jupiter.api.Assertions.assertEquals(0, rs.getBigDecimal(1)
                    .compareTo(new java.math.BigDecimal("0.30")),
                    "numeric(19,2) must hold exactly 0.30, not 0.30000000000000004");
        }
    }

    @Test
    void aProfileCannotDeclareACurrencyOrLocaleTheProductDoesNotSupport() {
        assertThrows(SQLException.class, () -> statement.execute(
                "insert into health_profiles (user_id, country_code, primary_currency, locale_tag,"
                        + " created_at, updated_at) values (999999,'BR','USD','pt-BR',current_timestamp,"
                        + "current_timestamp)"));
        assertThrows(SQLException.class, () -> statement.execute(
                "insert into health_profiles (user_id, country_code, primary_currency, locale_tag,"
                        + " created_at, updated_at) values (999998,'US','BRL','en-US',current_timestamp,"
                        + "current_timestamp)"));
    }
}
