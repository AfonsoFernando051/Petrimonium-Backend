package com.jf.PetApp.infrastructure.repository.academy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AcademyLessonStepTranslationJpaEntity;

@Repository
public interface AcademyLessonStepTranslationJpaRepository extends JpaRepository<AcademyLessonStepTranslationJpaEntity, Long> {

    List<AcademyLessonStepTranslationJpaEntity> findByStepId(Long stepId);

    List<AcademyLessonStepTranslationJpaEntity> findByLang(String lang);

    void deleteByStepId(Long stepId);
}
