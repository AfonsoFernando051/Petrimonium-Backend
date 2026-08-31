package com.jf.PetApp.infrastructure.repository.learning;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.jf.PetApp.application.learning.port.LearningCatalogPort;
import com.jf.PetApp.core.domain.learning.LessonCatalogEntry;
import com.jf.PetApp.core.domain.learning.ModuleCatalogEntry;
import com.jf.PetApp.infrastructure.entity.LearningLessonJpaEntity;
import com.jf.PetApp.infrastructure.entity.LearningModuleJpaEntity;

/**
 * The only place in the codebase that knows the learning catalog is stored
 * as JPA entities. Implements {@link LearningCatalogPort} so every use case
 * upstream works with plain domain records instead.
 */
@Repository
public class LearningCatalogRepositoryAdapter implements LearningCatalogPort {

    private final LearningLessonJpaRepository lessonRepository;
    private final LearningModuleJpaRepository moduleRepository;

    public LearningCatalogRepositoryAdapter(LearningLessonJpaRepository lessonRepository, LearningModuleJpaRepository moduleRepository) {
        this.lessonRepository = lessonRepository;
        this.moduleRepository = moduleRepository;
    }

    @Override
    public Optional<LessonCatalogEntry> findLesson(String lessonId) {
        return lessonRepository.findById(lessonId).map(this::toDomain);
    }

    @Override
    public Optional<ModuleCatalogEntry> findModule(String moduleId) {
        return moduleRepository.findById(moduleId).map(this::toDomain);
    }

    @Override
    public List<String> lessonIdsForModule(String moduleId) {
        return lessonRepository.findByModuleId(moduleId).stream()
                .map(LearningLessonJpaEntity::getLessonId)
                .toList();
    }

    @Override
    public Map<String, List<String>> lessonIdsGroupedByModule() {
        return lessonRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        LearningLessonJpaEntity::getModuleId,
                        Collectors.mapping(LearningLessonJpaEntity::getLessonId, Collectors.toList())));
    }

    @Override
    public List<ModuleCatalogEntry> allModules() {
        return moduleRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    private LessonCatalogEntry toDomain(LearningLessonJpaEntity entity) {
        return new LessonCatalogEntry(entity.getLessonId(), entity.getModuleId(), entity.getXpReward(), entity.getLessonOrder());
    }

    private ModuleCatalogEntry toDomain(LearningModuleJpaEntity entity) {
        return new ModuleCatalogEntry(entity.getModuleId(), entity.getXpReward(), entity.getModuleOrder(), entity.getLessonCount());
    }
}
