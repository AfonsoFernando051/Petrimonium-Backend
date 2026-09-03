package db.migration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Runs the real Flyway migrations (db/migration only -- prod's location list, not the dev seed
 * data) against a scratch in-memory H2 database, the same way {@code spring-boot:run} does
 * against the dev profile. {@code application.properties} sets {@code spring.flyway.enabled=false}
 * for the rest of the test suite deliberately (see its comment) so {@code @SpringBootTest} never
 * exercises these files -- which is exactly how V14 originally shipped with a migration that
 * failed the moment the app was actually started (H2 names an inline CHECK constraint
 * differently than Postgres does, so a hardcoded DROP CONSTRAINT name only worked by accident on
 * whichever engine it was guessed against). This test exists so that gap doesn't recur silently.
 *
 * <p>Everything -- Flyway's own DDL and this test's verification inserts -- runs through one
 * {@link SingleConnectionDataSource}. A plain H2 in-memory URL (even with {@code DB_CLOSE_DELAY=-1})
 * is only reliably alive across separate {@code DriverManager} connections once at least one
 * connection has stayed open the whole time; without that, a constraint added via {@code ALTER
 * TABLE} at runtime can be evaluated against a session that's already gone.
 */
class FlywayMigrationTest {

    @Test
    void allMigrationsApplyCleanlyToAFreshDatabase() {
        String url = "jdbc:h2:mem:flyway-migration-test-" + System.nanoTime();
        try (SingleConnectionDataSource dataSource = new SingleConnectionDataSource(url, "sa", "", true)) {
            Flyway flyway = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();

            assertDoesNotThrow(flyway::migrate, "every migration file, including Java ones, must apply without error");

            try (Statement statement = dataSource.getConnection().createStatement()) {
                assertAcceptsSpecie(statement, "OWL");
                assertRejectsSpecie(statement, "DRAGON");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertAcceptsSpecie(Statement statement, String specie) throws SQLException {
        statement.execute(
                "insert into jf_users (email, password, is_active) values "
                        + "('accept-" + specie + "@example.com', 'x', true)");
        long userId = lastGeneratedId(statement, "jf_users", "user_id");
        assertDoesNotThrow(
                () ->
                        statement.execute(
                                "insert into jf_pets (name, health, specie, user_id) values "
                                        + "('Coruja', 100, '" + specie + "', " + userId + ")"),
                "the V28 constraint must accept '" + specie + "'");
    }

    private void assertRejectsSpecie(Statement statement, String specie) throws SQLException {
        statement.execute(
                "insert into jf_users (email, password, is_active) values "
                        + "('reject-" + specie + "@example.com', 'x', true)");
        long userId = lastGeneratedId(statement, "jf_users", "user_id");
        assertTrue(
                isRejected(statement, specie, userId),
                "the constraint must still reject an unlisted specie ('" + specie + "')");
    }

    private boolean isRejected(Statement statement, String specie, long userId) {
        try {
            statement.execute(
                    "insert into jf_pets (name, health, specie, user_id) values "
                            + "('Invalid', 100, '" + specie + "', " + userId + ")");
            return false;
        } catch (SQLException expected) {
            return true;
        }
    }

    private long lastGeneratedId(Statement statement, String table, String idColumn) throws SQLException {
        try (var rs = statement.executeQuery("select max(" + idColumn + ") as id from " + table)) {
            rs.next();
            return rs.getLong("id");
        }
    }
}
