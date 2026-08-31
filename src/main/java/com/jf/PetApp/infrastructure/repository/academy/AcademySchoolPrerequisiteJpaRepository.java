package com.jf.PetApp.infrastructure.repository.academy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AcademySchoolPrerequisiteJpaEntity;

@Repository
public interface AcademySchoolPrerequisiteJpaRepository extends JpaRepository<AcademySchoolPrerequisiteJpaEntity, Long> {

    List<AcademySchoolPrerequisiteJpaEntity> findBySchoolId(String schoolId);

    void deleteBySchoolId(String schoolId);
}
