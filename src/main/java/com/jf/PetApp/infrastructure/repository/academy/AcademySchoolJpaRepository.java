package com.jf.PetApp.infrastructure.repository.academy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AcademySchoolJpaEntity;

@Repository
public interface AcademySchoolJpaRepository extends JpaRepository<AcademySchoolJpaEntity, String> {

    List<AcademySchoolJpaEntity> findByDomainIdOrderByOrderIndexAsc(String domainId);
}
