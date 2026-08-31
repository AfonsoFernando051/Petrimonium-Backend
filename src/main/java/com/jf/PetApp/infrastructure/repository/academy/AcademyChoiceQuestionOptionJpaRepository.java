package com.jf.PetApp.infrastructure.repository.academy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AcademyChoiceQuestionOptionJpaEntity;

@Repository
public interface AcademyChoiceQuestionOptionJpaRepository extends JpaRepository<AcademyChoiceQuestionOptionJpaEntity, Long> {

    List<AcademyChoiceQuestionOptionJpaEntity> findByStepIdOrderByPositionAsc(Long stepId);

    void deleteByStepId(Long stepId);
}
