package com.jf.PetApp.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.Test;

/**
 * Enforces the one module boundary that must never regress in this
 * codebase: an Academy operation must never be able to touch real_portfolio
 * data, and a real Wallet position must never be reachable from Academy's
 * code path (docs/BACKEND_MODULE_PLAN.md §2/§4). Package restructuring
 * (moving every context under {@code com.jf.PetApp.<context>}) is still
 * deferred, so this rule is scoped to the packages/class names that already
 * exist rather than waiting for that restructuring to write it.
 *
 * <p>Deliberate, documented exception: both contexts depend on
 * {@code ExternalInvestmentApiPort} (public market-data quotes) — that's
 * read-only provider integration, not portfolio state, so it's allowed on
 * both sides for now. Revisit if/when that port moves to a neutral
 * {@code shared}/market-data package (see PlaceSimulatedOrderUseCaseImpl's
 * class doc).
 */
class SimulatedPortfolioBoundaryTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.jf.PetApp");

    private static final String[] SIMULATED_PACKAGES = {
            "..application.simulatedportfolio..",
            "..infrastructure.controller.simulatedportfolio..",
            "..infrastructure.repository.simulatedportfolio.."
    };

    private static final String[] REAL_PACKAGES = {
            "..application.investment..",
            "..infrastructure.controller.investment..",
            "..infrastructure.repository.investment.."
    };

    // Deliberate, narrow exception: both contexts depend on this port for public market-data
    // quotes (read-only provider integration, not portfolio state) — see
    // PlaceSimulatedOrderUseCaseImpl's class doc and the class-level javadoc above.
    private static final DescribedPredicate<JavaClass> MARKET_DATA_PORT_EXCEPTION =
            JavaClass.Predicates.simpleName("ExternalInvestmentApiPort")
                    .or(JavaClass.Predicates.simpleName("AssetQuoteResponse"));

    @Test
    void simulatedPortfolioMustNotDependOnRealPortfolio() {
        ArchRule rule = noClasses().that().resideInAnyPackage(SIMULATED_PACKAGES)
                .should().dependOnClassesThat(
                        JavaClass.Predicates.resideInAnyPackage(REAL_PACKAGES)
                                .and(DescribedPredicate.not(MARKET_DATA_PORT_EXCEPTION)))
                .orShould().dependOnClassesThat().haveSimpleName("InvestmentJpaEntity")
                .orShould().dependOnClassesThat().haveSimpleName("FinanceJpaEntity")
                .orShould().dependOnClassesThat().haveSimpleName("Investment")
                .orShould().dependOnClassesThat().haveSimpleName("Finance")
                .because("simulated_portfolio (Academy) must never read or write real_portfolio "
                        + "(Wallet) data, except the shared read-only market-data quote port — "
                        + "see docs/BACKEND_MODULE_PLAN.md §2");

        rule.check(CLASSES);
    }

    @Test
    void realPortfolioMustNotDependOnSimulatedPortfolio() {
        ArchRule rule = noClasses().that().resideInAnyPackage(REAL_PACKAGES)
                .should().dependOnClassesThat().resideInAnyPackage(SIMULATED_PACKAGES)
                .orShould().dependOnClassesThat().haveSimpleName("SimulatedPortfolioJpaEntity")
                .orShould().dependOnClassesThat().haveSimpleName("SimulatedPositionJpaEntity")
                .orShould().dependOnClassesThat().haveSimpleName("SimulatedOrderJpaEntity")
                .orShould().dependOnClassesThat().haveSimpleName("SimulatedPortfolio")
                .orShould().dependOnClassesThat().haveSimpleName("SimulatedPosition")
                .orShould().dependOnClassesThat().haveSimpleName("SimulatedOrder")
                .because("real_portfolio (Wallet) must never read or write simulated_portfolio "
                        + "(Academy) data — see docs/BACKEND_MODULE_PLAN.md §2");

        rule.check(CLASSES);
    }
}
