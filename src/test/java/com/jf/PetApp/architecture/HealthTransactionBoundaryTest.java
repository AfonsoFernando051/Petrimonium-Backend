package com.jf.PetApp.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Every {@code HealthService} entry point that can reach a write must be {@code @Transactional}.
 *
 * <p>Exists because {@code listTransactions} wasn't (DEM-114). It reads, but it materializes the
 * month's recurrences first, so it writes — and the write was three calls deep behind a private
 * helper, which is exactly why nobody noticed. Its sibling {@code summary()} does the same thing
 * and was annotated; the difference was invisible in review.
 *
 * <p>Checking the reachable call graph rather than direct calls is the whole point: a rule that
 * only looked at methods calling {@code store.create...} directly would have passed on the very
 * bug it is meant to catch.
 */
class HealthTransactionBoundaryTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.jf.PetApp.application.health");

    /**
     * A {@code HealthStore} call that changes state. Matched by name prefix: the port is a
     * hand-written interface with a consistent naming convention, and a new mutating method that
     * doesn't follow it would be the anomaly worth catching in review anyway.
     */
    private static final List<String> MUTATING_PREFIXES =
            List.of("create", "save", "update", "delete", "archive", "mark", "deactivate", "insert");

    private static boolean isMutatingStoreCall(JavaMethodCall call) {
        String targetOwner = call.getTargetOwner().getName();
        if (!targetOwner.endsWith("HealthStore")) {
            return false;
        }
        String name = call.getTarget().getName().toLowerCase(Locale.ROOT);
        return MUTATING_PREFIXES.stream().anyMatch(name::startsWith);
    }

    /** Walks calls within {@code owner} from {@code entry}, reporting whether any of them writes. */
    private static boolean reachesAWrite(JavaClass owner, JavaMethod entry) {
        Set<String> seen = new HashSet<>();
        Deque<JavaMethod> queue = new ArrayDeque<>();
        queue.add(entry);
        seen.add(entry.getFullName());

        while (!queue.isEmpty()) {
            JavaMethod current = queue.removeFirst();
            for (JavaMethodCall call : current.getMethodCallsFromSelf()) {
                if (isMutatingStoreCall(call)) {
                    return true;
                }
                // Follow calls that stay inside the service, so a write hidden behind a private
                // helper still counts against the public method that can reach it.
                if (!call.getTargetOwner().equals(owner)) {
                    continue;
                }
                call.getTarget().resolveMember().ifPresent(member -> {
                    if (member instanceof JavaMethod method && seen.add(method.getFullName())) {
                        queue.add(method);
                    }
                });
            }
        }
        return false;
    }

    @Test
    void everyHealthServiceMethodThatCanReachAWriteIsTransactional() {
        JavaClass service = CLASSES.get("com.jf.PetApp.application.health.HealthService");

        List<String> offenders = new ArrayList<>();
        for (JavaMethod method : service.getMethods()) {
            if (!method.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.PUBLIC)) {
                continue;
            }
            if (!reachesAWrite(service, method)) {
                continue;
            }
            boolean transactional = method.isAnnotatedWith(Transactional.class)
                    || service.isAnnotatedWith(Transactional.class);
            if (!transactional) {
                offenders.add(method.getName());
            }
        }

        assertTrue(offenders.isEmpty(),
                "these HealthService methods can reach a HealthStore write but are not @Transactional, "
                        + "so a partial failure cannot roll back and concurrent callers can race a "
                        + "unique constraint: " + offenders);
    }

    /**
     * Guards the guard: if the walk stopped finding writes — a renamed port, a refactor that moved
     * the store behind another collaborator — the rule above would pass vacuously and stop
     * protecting anything.
     */
    @Test
    void theRuleActuallyFindsWritingMethods() {
        JavaClass service = CLASSES.get("com.jf.PetApp.application.health.HealthService");

        long writers = service.getMethods().stream()
                .filter(m -> m.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.PUBLIC))
                .filter(m -> reachesAWrite(service, m))
                .count();

        assertTrue(writers > 5,
                "expected the call-graph walk to still identify HealthService's writing methods, "
                        + "found " + writers + " — the rule may have gone vacuous");
    }
}
