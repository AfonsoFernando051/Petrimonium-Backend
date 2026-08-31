package com.jf.PetApp.infrastructure.repository.learning;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.learning.port.LessonProgressRepositoryPort;
import com.jf.PetApp.core.domain.learning.LessonProgressSnapshot;
import com.jf.PetApp.infrastructure.entity.LessonProgressJpaEntity;

/**
 * The only place in the codebase that knows lesson progress is stored as a
 * JPA entity. Implements {@link LessonProgressRepositoryPort} so every use
 * case upstream never touches Spring Data directly.
 */
@Repository
public class LessonProgressRepositoryAdapter implements LessonProgressRepositoryPort {

    private final LessonProgressJpaRepository repository;

    public LessonProgressRepositoryAdapter(LessonProgressJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isLessonCompleted(Long userId, String lessonId) {
        return repository.existsByUserIdAndLessonId(userId, lessonId);
    }

    @Override
    @Transactional
    public void markCompleted(Long userId, String lessonId, boolean perfectFirstTry) {
        LessonProgressJpaEntity entity = repository.findByUserIdAndLessonId(userId, lessonId).orElse(null);
        if (entity == null) {
            entity = new LessonProgressJpaEntity();
            entity.setUserId(userId);
            entity.setLessonId(lessonId);
            entity.setCompletedAt(Instant.now());
            entity.setPerfectFirstTry(perfectFirstTry);
            repository.save(entity);
            return;
        }
        if (perfectFirstTry && !entity.isPerfectFirstTry()) {
            entity.setPerfectFirstTry(true);
            repository.save(entity);
        }
    }

    @Override
    public Set<String> completedLessonIds(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(LessonProgressJpaEntity::getLessonId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> perfectLessonIds(Long userId) {
        return repository.findByUserId(userId).stream()
                .filter(LessonProgressJpaEntity::isPerfectFirstTry)
                .map(LessonProgressJpaEntity::getLessonId)
                .collect(Collectors.toSet());
    }

    @Override
    public LessonProgressSnapshot snapshotFor(Long userId) {
        List<LessonProgressJpaEntity> rows = repository.findByUserId(userId);
        Set<String> completedLessonIds = rows.stream()
                .map(LessonProgressJpaEntity::getLessonId)
                .collect(Collectors.toSet());
        Set<String> perfectLessonIds = rows.stream()
                .filter(LessonProgressJpaEntity::isPerfectFirstTry)
                .map(LessonProgressJpaEntity::getLessonId)
                .collect(Collectors.toSet());
        return new LessonProgressSnapshot(completedLessonIds, perfectLessonIds);
    }
}
