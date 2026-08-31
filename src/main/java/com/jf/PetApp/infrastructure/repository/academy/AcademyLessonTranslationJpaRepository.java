package com.jf.PetApp.infrastructure.repository.academy;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AcademyDomainJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademyLessonTranslationJpaEntity;

@Repository
public interface AcademyLessonTranslationJpaRepository extends JpaRepository<AcademyLessonTranslationJpaEntity, Long> {

    List<AcademyLessonTranslationJpaEntity> findByLessonId(String lessonId);

    List<AcademyLessonTranslationJpaEntity> findByLang(String lang);

    void deleteByLessonId(String lessonId);

    Optional<AcademyLessonTranslationJpaEntity> findByLessonIdAndLang(String lessonId, String lang);
}
