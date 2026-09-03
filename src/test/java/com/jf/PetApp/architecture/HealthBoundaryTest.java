package com.jf.PetApp.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.Test;

/**
 * Health is the third bounded context, and the same rule that keeps Academy's simulated money
 * away from Wallet's real positions (see {@link SimulatedPortfolioBoundaryTest}) applies to it in
 * both directions: the user's real cash flow must not be computed from Academy's practice data,
 * and no Academy or Wallet feature may quietly start reading a Health balance instead of going
 * through a deliberate, reviewed contract.
 *
 * <p>Unlike the simulated/real pair there is no market-data exception here — Health has no
 * provider integration at all in this release, and the future bank import is expected to arrive
 * as its own adapter behind {@code HealthStore}, not as a dependency on either neighbour.
 */
class HealthBoundaryTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.jf.PetApp");

    private static final String[] HEALTH_PACKAGES = {
            "..application.health..",
            "..core.domain.health..",
            "..infrastructure.controller.health..",
            "..infrastructure.repository.health.."
    };

    private static final String[] NEIGHBOUR_PACKAGES = {
            "..application.investment..",
            "..infrastructure.controller.investment..",
            "..infrastructure.repository.investment..",
            "..application.simulatedportfolio..",
            "..infrastructure.controller.simulatedportfolio..",
            "..infrastructure.repository.simulatedportfolio..",
            "..application.academy..",
            "..infrastructure.controller.academy..",
            "..infrastructure.repository.academy.."
    };

    @Test
    void healthMustNotDependOnWalletOrAcademy() {
        ArchRule rule = noClasses().that().resideInAnyPackage(HEALTH_PACKAGES)
                .should().dependOnClassesThat(JavaClass.Predicates.resideInAnyPackage(NEIGHBOUR_PACKAGES))
                .because("a real budget must never be derived from a simulated portfolio or from "
                        + "Wallet positions -- Health owns its own ledger");

        rule.check(CLASSES);
    }

    @Test
    void walletAndAcademyMustNotDependOnHealth() {
        ArchRule rule = noClasses().that().resideInAnyPackage(NEIGHBOUR_PACKAGES)
                .should().dependOnClassesThat(JavaClass.Predicates.resideInAnyPackage(HEALTH_PACKAGES))
                .because("Health holds the user's real spending; reaching it from another product's "
                        + "code path would bypass the app_context gate that protects it");

        rule.check(CLASSES);
    }
}
