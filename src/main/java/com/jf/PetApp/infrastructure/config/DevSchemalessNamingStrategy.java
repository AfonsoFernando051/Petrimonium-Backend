package com.jf.PetApp.infrastructure.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Dev-only (H2): ignores every entity's declared {@code @Table(schema = ...)},
 * keeping Spring Boot's default {@link PhysicalNamingStrategySnakeCaseImpl}
 * behavior (e.g. {@code isActive} -> {@code is_active}) for everything else.
 *
 * H2 cannot move a table into another schema via {@code ALTER TABLE} —
 * confirmed empirically ("Schema name must match" on a cross-schema RENAME)
 * — so the schema-per-context split (V20, {@code db/migration-postgres})
 * targets real PostgreSQL only and is excluded from dev's Flyway locations.
 * Dev's tables stay physically unqualified in H2's default schema, exactly
 * as {@code db/migration}/{@code db/migration-dev} actually create them;
 * this strategy makes {@code ddl-auto=validate} agree with that, instead of
 * expecting schemas that were never created here.
 */
public class DevSchemalessNamingStrategy extends PhysicalNamingStrategySnakeCaseImpl {

    @Override
    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment context) {
        return null;
    }
}
