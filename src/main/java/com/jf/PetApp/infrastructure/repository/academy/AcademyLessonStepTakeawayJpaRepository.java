package com.jf.PetApp.infrastructure.repository.academy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AcademyLessonStepTakeawayJpaEntity;

@Repository
public interface AcademyLessonStepTakeawayJpaRepository extends JpaRepository<AcademyLessonStepTakeawayJpaEntity, Long> {

    List<AcademyLessonStepTakeawayJpaEntity> findByStepIdOrderByPositionAsc(Long stepId);

    void deleteByStepId(Long stepId);
}
