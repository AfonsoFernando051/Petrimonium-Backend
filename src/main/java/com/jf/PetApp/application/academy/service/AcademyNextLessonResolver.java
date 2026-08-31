package com.jf.PetApp.application.academy.service;

import com.jf.PetApp.application.academy.dto.AcademyCatalogResult;
import com.jf.PetApp.application.academy.dto.AcademyDomainView;
import com.jf.PetApp.application.academy.dto.AcademyLessonView;
import com.jf.PetApp.application.academy.dto.AcademyModuleView;
import com.jf.PetApp.application.academy.dto.AcademySchoolView;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The next lesson a learner should continue with: the first not-yet-completed lesson of the
 * first unlocked, content-available module, scanning domain → school → module → lesson in
 * curriculum order. Mirrors {@code petapp_mobile/lib/features/academy/domain/services/
 * academy_progress_calculator.dart}'s {@code nextLessonToContinue} (including its
 * per-parent-scoped ordering and module-prerequisite lock rule) so the Mentor's view of "what's
 * next" never disagrees with what the Academy tab itself shows. Pure function, no framework
 * dependency, so it can be unit tested directly.
 */
public final class AcademyNextLessonResolver {

    private AcademyNextLessonResolver() {
    }

    public static Optional<AcademyLessonView> resolve(AcademyCatalogResult catalog, Set<String> completedLessonIds) {
        Map<String, AcademyModuleView> modulesById = catalog.modules().stream()
                .collect(Collectors.toMap(AcademyModuleView::id, m -> m));
        Map<String, List<AcademyLessonView>> lessonsByModule = catalog.lessons().stream()
                .collect(Collectors.groupingBy(AcademyLessonView::moduleId));
        Map<String, List<AcademySchoolView>> schoolsByDomain = catalog.schools().stream()
                .collect(Collectors.groupingBy(AcademySchoolView::domainId));
        Map<String, List<AcademyModuleView>> modulesBySchool = catalog.modules().stream()
                .collect(Collectors.groupingBy(AcademyModuleView::schoolId));

        List<AcademyDomainView> orderedDomains = catalog.domains().stream()
                .sorted(Comparator.comparingInt(AcademyDomainView::order))
                .toList();

        for (AcademyDomainView domain : orderedDomains) {
            List<AcademySchoolView> schools = schoolsByDomain.getOrDefault(domain.id(), List.of()).stream()
                    .sorted(Comparator.comparingInt(AcademySchoolView::order))
                    .toList();
            for (AcademySchoolView school : schools) {
                List<AcademyModuleView> modules = modulesBySchool.getOrDefault(school.id(), List.of()).stream()
                        .sorted(Comparator.comparingInt(AcademyModuleView::order))
                        .toList();
                for (AcademyModuleView module : modules) {
                    if (!module.contentAvailable() || isModuleLocked(module, modulesById, lessonsByModule, completedLessonIds)) {
                        continue;
                    }
                    List<AcademyLessonView> lessons = lessonsByModule.getOrDefault(module.id(), List.of()).stream()
                            .sorted(Comparator.comparingInt(AcademyLessonView::order))
                            .toList();
                    for (AcademyLessonView lesson : lessons) {
                        if (!completedLessonIds.contains(lesson.id())) {
                            return Optional.of(lesson);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isModuleLocked(AcademyModuleView module, Map<String, AcademyModuleView> modulesById,
            Map<String, List<AcademyLessonView>> lessonsByModule, Set<String> completedLessonIds) {
        return module.prerequisites().stream()
                .anyMatch(id -> !isModuleCompleted(modulesById.get(id), lessonsByModule, completedLessonIds));
    }

    private static boolean isModuleCompleted(AcademyModuleView module, Map<String, List<AcademyLessonView>> lessonsByModule,
            Set<String> completedLessonIds) {
        if (module == null || !module.contentAvailable()) {
            return false;
        }
        List<AcademyLessonView> lessons = lessonsByModule.getOrDefault(module.id(), List.of());
        return !lessons.isEmpty() && lessons.stream().allMatch(l -> completedLessonIds.contains(l.id()));
    }
}
