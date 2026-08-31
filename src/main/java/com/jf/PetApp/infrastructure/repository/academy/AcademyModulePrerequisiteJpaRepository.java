package com.jf.PetApp.infrastructure.repository.academy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AcademyModulePrerequisiteJpaEntity;

@Repository
public interface AcademyModulePrerequisiteJpaRepository extends JpaRepository<AcademyModulePrerequisiteJpaEntity, Long> {

    List<AcademyModulePrerequisiteJpaEntity> findByModuleId(String moduleId);

    void deleteByModuleId(String moduleId);
}
