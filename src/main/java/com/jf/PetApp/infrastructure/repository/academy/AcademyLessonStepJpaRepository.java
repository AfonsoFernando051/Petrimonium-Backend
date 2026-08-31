package com.jf.PetApp.infrastructure.repository.academy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AcademyLessonStepJpaEntity;

@Repository
public interface AcademyLessonStepJpaRepository extends JpaRepository<AcademyLessonStepJpaEntity, Long> {

    List<AcademyLessonStepJpaEntity> findByLessonIdOrderByStepOrderAsc(String lessonId);

    /**
     * Only deletes the step rows themselves — descendants (translations,
     * options + their translations, takeaways + their translations) are
     * not cascaded by JPA here and must be deleted first by the caller
     * ({@code AcademyContentSeedRunner.deleteStepChildren}). The schema's
     * {@code ON DELETE CASCADE} (see V10) is a safety net for rows written
     * outside the seeder, not something application code relies on — the
     * test database schema (Hibernate {@code ddl-auto}, not Flyway) doesn't
     * derive that constraint from these plain, unmapped foreign-key columns.
     */
    void deleteByLessonId(String lessonId);
}
