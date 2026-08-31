package com.jf.PetApp.application.academy.service;

import com.jf.PetApp.application.academy.dto.AcademyCatalogResult;
import com.jf.PetApp.application.academy.dto.AcademyDomainView;
import com.jf.PetApp.application.academy.dto.AcademyLessonView;
import com.jf.PetApp.application.academy.dto.AcademyModuleView;
import com.jf.PetApp.application.academy.dto.AcademySchoolView;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademyNextLessonResolverTest {

    private static AcademyDomainView domain(String id, int order, String... schoolIds) {
        return new AcademyDomainView(id, id, null, null, order, List.of(schoolIds));
    }

    private static AcademySchoolView school(String id, String domainId, int order, List<String> prerequisites) {
        return new AcademySchoolView(id, domainId, id, null, null, order, prerequisites, true);
    }

    private static AcademyModuleView module(String id, String schoolId, int order, List<String> lessonIds,
            List<String> prerequisites) {
        return new AcademyModuleView(id, schoolId, id, null, null, "BEGINNER", order, lessonIds, prerequisites, true);
    }

    private static AcademyModuleView moduleWithoutContent(String id, String schoolId, int order) {
        return new AcademyModuleView(id, schoolId, id, null, null, "BEGINNER", order, List.of(), List.of(), false);
    }

    private static AcademyLessonView lesson(String id, String moduleId, int order) {
        return new AcademyLessonView(id, moduleId, id, null, null, null, order, 10, List.of(), List.of());
    }

    @Test
    void resolve_WithNothingCompleted_ReturnsFirstLessonOfFirstModule() {
        AcademyCatalogResult catalog = new AcademyCatalogResult(
                List.of(domain("dom-1", 1, "school-1")),
                List.of(school("school-1", "dom-1", 1, List.of())),
                List.of(module("module-1", "school-1", 1, List.of("lesson-1", "lesson-2"), List.of())),
                List.of(lesson("lesson-1", "module-1", 1), lesson("lesson-2", "module-1", 2)));

        Optional<AcademyLessonView> next = AcademyNextLessonResolver.resolve(catalog, Set.of());

        assertTrue(next.isPresent());
        assertEquals("lesson-1", next.get().id());
    }

    @Test
    void resolve_SkipsLessonsAlreadyCompleted() {
        AcademyCatalogResult catalog = new AcademyCatalogResult(
                List.of(domain("dom-1", 1, "school-1")),
                List.of(school("school-1", "dom-1", 1, List.of())),
                List.of(module("module-1", "school-1", 1, List.of("lesson-1", "lesson-2"), List.of())),
                List.of(lesson("lesson-1", "module-1", 1), lesson("lesson-2", "module-1", 2)));

        Optional<AcademyLessonView> next = AcademyNextLessonResolver.resolve(catalog, Set.of("lesson-1"));

        assertTrue(next.isPresent());
        assertEquals("lesson-2", next.get().id());
    }

    @Test
    void resolve_SkipsModulesWithoutContentAvailable() {
        AcademyCatalogResult catalog = new AcademyCatalogResult(
                List.of(domain("dom-1", 1, "school-1")),
                List.of(school("school-1", "dom-1", 1, List.of())),
                List.of(
                        moduleWithoutContent("module-1", "school-1", 1),
                        module("module-2", "school-1", 2, List.of("lesson-2"), List.of())),
                List.of(lesson("lesson-2", "module-2", 1)));

        Optional<AcademyLessonView> next = AcademyNextLessonResolver.resolve(catalog, Set.of());

        assertTrue(next.isPresent());
        assertEquals("lesson-2", next.get().id());
    }

    @Test
    void resolve_SkipsModuleWhosePrerequisiteModuleIsIncomplete() {
        AcademyCatalogResult catalog = new AcademyCatalogResult(
                List.of(domain("dom-1", 1, "school-1")),
                List.of(school("school-1", "dom-1", 1, List.of())),
                List.of(
                        module("module-1", "school-1", 1, List.of("lesson-1"), List.of()),
                        module("module-2", "school-1", 2, List.of("lesson-2"), List.of("module-1"))),
                List.of(lesson("lesson-1", "module-1", 1), lesson("lesson-2", "module-2", 1)));

        // module-1 (lesson-1) not completed yet, so module-2 stays locked and is skipped —
        // module-1's own lesson-1 is what should surface.
        Optional<AcademyLessonView> next = AcademyNextLessonResolver.resolve(catalog, Set.of());

        assertTrue(next.isPresent());
        assertEquals("lesson-1", next.get().id());
    }

    @Test
    void resolve_UnlocksModuleOncePrerequisiteModuleIsFullyCompleted() {
        AcademyCatalogResult catalog = new AcademyCatalogResult(
                List.of(domain("dom-1", 1, "school-1")),
                List.of(school("school-1", "dom-1", 1, List.of())),
                List.of(
                        module("module-1", "school-1", 1, List.of("lesson-1"), List.of()),
                        module("module-2", "school-1", 2, List.of("lesson-2"), List.of("module-1"))),
                List.of(lesson("lesson-1", "module-1", 1), lesson("lesson-2", "module-2", 1)));

        Optional<AcademyLessonView> next = AcademyNextLessonResolver.resolve(catalog, Set.of("lesson-1"));

        assertTrue(next.isPresent());
        assertEquals("lesson-2", next.get().id());
    }

    @Test
    void resolve_OrdersByDomainThenSchoolThenModule_NotByGloballyTiedModuleOrder() {
        // Both modules use order=1 — a per-parent-scoped field, not globally unique — so only
        // walking domain -> school -> module in hierarchy order picks the right one.
        AcademyCatalogResult catalog = new AcademyCatalogResult(
                List.of(domain("dom-1", 2, "school-b"), domain("dom-0", 1, "school-a")),
                List.of(
                        school("school-b", "dom-1", 1, List.of()),
                        school("school-a", "dom-0", 1, List.of())),
                List.of(
                        module("module-b", "school-b", 1, List.of("lesson-b"), List.of()),
                        module("module-a", "school-a", 1, List.of("lesson-a"), List.of())),
                List.of(lesson("lesson-b", "module-b", 1), lesson("lesson-a", "module-a", 1)));

        Optional<AcademyLessonView> next = AcademyNextLessonResolver.resolve(catalog, Set.of());

        assertTrue(next.isPresent());
        assertEquals("lesson-a", next.get().id());
    }

    @Test
    void resolve_WhenEveryLessonIsCompleted_ReturnsEmpty() {
        AcademyCatalogResult catalog = new AcademyCatalogResult(
                List.of(domain("dom-1", 1, "school-1")),
                List.of(school("school-1", "dom-1", 1, List.of())),
                List.of(module("module-1", "school-1", 1, List.of("lesson-1"), List.of())),
                List.of(lesson("lesson-1", "module-1", 1)));

        Optional<AcademyLessonView> next = AcademyNextLessonResolver.resolve(catalog, Set.of("lesson-1"));

        assertTrue(next.isEmpty());
    }

    @Test
    void resolve_WithEmptyCatalog_ReturnsEmpty() {
        AcademyCatalogResult catalog = new AcademyCatalogResult(List.of(), List.of(), List.of(), List.of());

        assertTrue(AcademyNextLessonResolver.resolve(catalog, Set.of()).isEmpty());
    }
}
