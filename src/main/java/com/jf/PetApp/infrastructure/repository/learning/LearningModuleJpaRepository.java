package com.jf.PetApp.infrastructure.repository.learning;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.LearningModuleJpaEntity;

@Repository
public interface LearningModuleJpaRepository extends JpaRepository<LearningModuleJpaEntity, String> {
}
