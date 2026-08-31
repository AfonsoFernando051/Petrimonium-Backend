package com.jf.PetApp.infrastructure.repository.academy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AcademyLessonPortfolioConceptJpaEntity;

@Repository
public interface AcademyLessonPortfolioConceptJpaRepository extends JpaRepository<AcademyLessonPortfolioConceptJpaEntity, Long> {

    List<AcademyLessonPortfolioConceptJpaEntity> findByLessonId(String lessonId);

    void deleteByLessonId(String lessonId);
}
