package com.jf.PetApp.infrastructure.repository.learning;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.LearningLessonJpaEntity;

@Repository
public interface LearningLessonJpaRepository extends JpaRepository<LearningLessonJpaEntity, String> {
    List<LearningLessonJpaEntity> findByModuleId(String moduleId);
}
