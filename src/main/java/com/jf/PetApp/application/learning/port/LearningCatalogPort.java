package com.jf.PetApp.application.learning.port;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.jf.PetApp.core.domain.learning.LessonCatalogEntry;
import com.jf.PetApp.core.domain.learning.ModuleCatalogEntry;

/**
 * Read-only access to the server's lesson/module catalog (seeded via
 * Flyway — see V2__learning_gamification_schema.sql). Use cases depend on
 * this port, never on Spring Data or JPA entities directly.
 */
public interface LearningCatalogPort {

    Optional<LessonCatalogEntry> findLesson(String lessonId);

    Optional<ModuleCatalogEntry> findModule(String moduleId);

    List<String> lessonIdsForModule(String moduleId);

    /**
     * Lesson ids for every module, keyed by module id, in a single fetch.
     * Use this instead of calling {@link #lessonIdsForModule(String)} in a
     * loop over all modules.
     */
    Map<String, List<String>> lessonIdsGroupedByModule();

    List<ModuleCatalogEntry> allModules();
}
