package db.migration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Trava a deriva mais perigosa desta base: alguém acrescenta uma tabela com
 * {@code user_id} e ninguém se lembra de a limpar ao apagar a conta. O
 * resultado seria silencioso — a conta sai, os dados ficam órfãos, e nenhum
 * teste existente repara.
 *
 * <p>Este teste lê as tabelas reais de uma base migrada e exige que cada uma
 * esteja declarada abaixo, com o sítio onde é tratada. Acrescentar uma tabela
 * obriga a passar por aqui e a decidir conscientemente.
 */
class UserDataErasureCoverageTest {

    /** Apagadas explicitamente pelo {@code UserDataEraser}. */
    private static final Set<String> ERASED_EXPLICITLY = Set.of(
        "achievement_unlocks", "activity_log", "lesson_progress", "mission_completions",
        "xp_events", "jf_investments", "jf_pets", "jf_pet_app_links", "jf_mentor_conversations", "jf_refresh_tokens",
        "jf_password_reset_tokens", "simulated_portfolios",
        "health_profiles", "health_accounts", "health_recurrences", "health_cards",
        "health_card_invoices", "health_card_purchases", "health_card_installments",
        "health_transfers", "health_transactions");

    /** Saem por cascata do {@code UserJpaEntity} (@OneToOne orphanRemoval). */
    private static final Set<String> ERASED_BY_JPA_CASCADE = Set.of("jf_finances");

    /** A própria linha da conta, apagada no fim pelo use case. */
    private static final Set<String> THE_ACCOUNT_ITSELF = Set.of("jf_users");

    @Test
    void everyTableHoldingUserDataIsAccountedForOnDeletion() throws Exception {
        String url = "jdbc:h2:mem:erasure-coverage-" + System.nanoTime();
        try (SingleConnectionDataSource dataSource = new SingleConnectionDataSource(url, "sa", "", true)) {
            Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

            Set<String> naBase = new TreeSet<>();
            try (Statement statement = dataSource.getConnection().createStatement()) {
                ResultSet rs = statement.executeQuery(
                    "select table_name from information_schema.columns "
                        + "where lower(column_name) = 'user_id'");
                while (rs.next()) {
                    naBase.add(rs.getString(1).toLowerCase(Locale.ROOT));
                }
            }

            Set<String> cobertas = new TreeSet<>(ERASED_EXPLICITLY);
            cobertas.addAll(ERASED_BY_JPA_CASCADE);
            cobertas.addAll(THE_ACCOUNT_ITSELF);

            Set<String> semTratamento = new TreeSet<>(naBase);
            semTratamento.removeAll(cobertas);
            assertTrue(
                semTratamento.isEmpty(),
                "tabelas com user_id que ninguém apaga ao remover a conta: " + semTratamento
                    + " — trate-as no UserDataEraser e declare-as neste teste");

            Set<String> declaradasQueNaoExistem = new TreeSet<>(cobertas);
            declaradasQueNaoExistem.removeAll(naBase);
            assertTrue(
                declaradasQueNaoExistem.isEmpty(),
                "declaradas aqui mas sem coluna user_id na base: " + declaradasQueNaoExistem);
        }
    }
}
