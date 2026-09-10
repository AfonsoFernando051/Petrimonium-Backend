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
 * Every Health use case that can reach a write must be {@code @Transactional}.
 *
 * <p>Exists because {@code listTransactions} wasn't (DEM-114). It reads, but it materializes the
 * month's recurrences first, so it writes — and the write was three calls deep behind a private
 * helper, which is exactly why nobody noticed. Its sibling {@code summary()} does the same thing
 * and was annotated; the difference was invisible in review.
 *
 * <p>Checking the reachable call graph rather than direct calls is the whole point: a rule that
 * only looked at methods calling {@code store.create...} directly would have passed on the very
 * bug it is meant to catch.
 *
 * <p>Since the split into use cases the walk must also follow calls into the collaborators in
 * {@code application.health.service} — {@code RecurrenceMaterializer} is precisely where the
 * DEM-114 write now lives, so a walk that stopped at the class boundary would go vacuous on the
 * original bug. {@link #theRuleActuallyFindsWritingMethods} is what keeps that honest.
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

    /** True for anything inside the Health slice: a use case impl or one of its collaborators. */
    private static boolean insideHealthSlice(JavaMethodCall call) {
        return call.getTargetOwner().getPackageName().startsWith("com.jf.PetApp.application.health");
    }

    /** Walks calls reachable from {@code entry} inside the Health slice, reporting whether any writes. */
    private static boolean reachesAWrite(JavaMethod entry) {
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
                // Follow calls that stay inside the Health slice, so a write hidden behind a
                // private helper or a collaborator still counts against the use case that can
                // reach it.
                if (!insideHealthSlice(call)) {
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

    /** Every {@code execute} on a Health use case implementation. */
    private static List<JavaMethod> useCaseEntryPoints() {
        List<JavaMethod> entries = new ArrayList<>();
        for (JavaClass type : CLASSES) {
            if (!type.getSimpleName().endsWith("UseCaseImpl")) {
                continue;
            }
            type.getMethods().stream()
                    .filter(m -> m.getName().equals("execute"))
                    .forEach(entries::add);
        }
        return entries;
    }

    @Test
    void everyHealthUseCaseThatCanReachAWriteIsTransactional() {
        List<String> offenders = new ArrayList<>();
        for (JavaMethod entry : useCaseEntryPoints()) {
            if (!reachesAWrite(entry)) {
                continue;
            }
            boolean transactional = entry.isAnnotatedWith(Transactional.class)
                    || entry.getOwner().isAnnotatedWith(Transactional.class);
            if (!transactional) {
                offenders.add(entry.getOwner().getSimpleName());
            }
        }

        assertTrue(offenders.isEmpty(),
                "these Health use cases can reach a HealthStore write but are not @Transactional, "
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
        long writers = useCaseEntryPoints().stream().filter(HealthTransactionBoundaryTest::reachesAWrite).count();

        assertTrue(writers > 5,
                "expected the call-graph walk to still identify Health's writing use cases, "
                        + "found " + writers + " — the rule may have gone vacuous");
    }
}
