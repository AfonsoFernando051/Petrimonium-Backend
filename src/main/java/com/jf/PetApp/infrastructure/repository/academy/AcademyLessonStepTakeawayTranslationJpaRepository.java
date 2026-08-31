package com.jf.PetApp.infrastructure.repository.academy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AcademyLessonStepTakeawayTranslationJpaEntity;

@Repository
public interface AcademyLessonStepTakeawayTranslationJpaRepository
        extends JpaRepository<AcademyLessonStepTakeawayTranslationJpaEntity, Long> {

    List<AcademyLessonStepTakeawayTranslationJpaEntity> findByTakeawayId(Long takeawayId);

    List<AcademyLessonStepTakeawayTranslationJpaEntity> findByLang(String lang);

    void deleteByTakeawayId(Long takeawayId);
}
