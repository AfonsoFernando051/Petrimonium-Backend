package com.jf.PetApp.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.Test;

/**
 * The dependency-direction rule documented at the top of the repo's AGENTS.md — {@code
 * core/domain} never imports {@code application} or {@code infrastructure}; {@code application}
 * never imports {@code infrastructure} — had no machine-checked test of its own before this one.
 * Only the bounded-context boundaries (Health vs Wallet/Academy, simulated vs real portfolio) were
 * covered; nothing stopped a use case from quietly importing a concrete repository/entity/adapter
 * class the way {@code DeleteAccountUseCaseImpl} did with {@code UserDataEraser} until that was
 * fixed by introducing {@code UserDataErasurePort}.
 */
class LayeringTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.jf.PetApp");

    @Test
    void applicationMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("application depends on ports it declares itself; infrastructure supplies "
                        + "the adapters, and the dependency must point inward, never back out");

        rule.check(CLASSES);
    }

    @Test
    void coreDomainMustNotDependOnApplicationOrInfrastructure() {
        ArchRule rule = noClasses().that().resideInAPackage("..core.domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..")
                .because("core/domain is the innermost layer -- plain entities with zero framework "
                        + "or use-case dependency -- so nothing outside it may leak in");

        rule.check(CLASSES);
    }
}
