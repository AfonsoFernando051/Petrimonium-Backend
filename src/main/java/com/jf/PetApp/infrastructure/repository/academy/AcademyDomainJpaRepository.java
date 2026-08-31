package com.jf.PetApp.infrastructure.repository.academy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AcademyDomainJpaEntity;

@Repository
public interface AcademyDomainJpaRepository extends JpaRepository<AcademyDomainJpaEntity, String> {

    List<AcademyDomainJpaEntity> findAllByOrderByOrderIndexAsc();
}
