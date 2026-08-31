package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Widens {@code xp_events.event_type}'s CHECK constraint (from
 * V4__learning_gamification_schema.sql) to allow {@code SIMULATOR_COMPLETED}
 * — the new event type backing Financial Lab simulator XP (DECISION-037).
 *
 * <p>A Java migration for the same reason as {@link V14__AddOwlPetSpecie}:
 * the constraint was created inline (no explicit name) in V4, and Postgres
 * (prod) and H2 (dev) name it differently — confirmed for this exact
 * constraint by booting an in-memory H2 instance and inspecting
 * {@code information_schema.check_constraints}, which named it an opaque,
 * creation-order-dependent {@code CONSTRAINT_N}, not Postgres's
 * deterministic {@code xp_events_event_type_check}. Looked up at runtime via
 * the SQL-standard {@code information_schema.table_constraints} view both
 * engines expose, with a case-insensitive table-name match since H2 and
 * Postgres fold unquoted identifiers to opposite cases.
 */
public class V19__XpEventsSimulatorEventType extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String constraintName = findXpEventsCheckConstraintName(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute("alter table xp_events drop constraint " + constraintName);
            statement.execute(
                    "alter table xp_events add constraint xp_events_event_type_check "
                            + "check (event_type in ('LESSON_COMPLETED', 'MODULE_COMPLETED', 'SIMULATOR_COMPLETED'))");
        }
    }

    private String findXpEventsCheckConstraintName(Connection connection) throws Exception {
        String sql =
                "select constraint_name from information_schema.table_constraints "
                        + "where upper(table_name) = 'XP_EVENTS' and constraint_type = 'CHECK'";
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next()) {
                throw new IllegalStateException(
                        "Expected exactly one CHECK constraint on xp_events (the event_type check "
                                + "from V4__learning_gamification_schema.sql), found none.");
            }
            return resultSet.getString("constraint_name");
        }
    }
}
