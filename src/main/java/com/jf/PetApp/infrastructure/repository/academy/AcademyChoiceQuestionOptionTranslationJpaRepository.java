package com.jf.PetApp.infrastructure.repository.academy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AcademyChoiceQuestionOptionTranslationJpaEntity;

@Repository
public interface AcademyChoiceQuestionOptionTranslationJpaRepository
        extends JpaRepository<AcademyChoiceQuestionOptionTranslationJpaEntity, Long> {

    List<AcademyChoiceQuestionOptionTranslationJpaEntity> findByOptionId(Long optionId);

    List<AcademyChoiceQuestionOptionTranslationJpaEntity> findByLang(String lang);

    void deleteByOptionId(Long optionId);
}
