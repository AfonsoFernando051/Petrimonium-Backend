package com.jf.PetApp.infrastructure.repository.academy;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AcademyDomainJpaEntity;
import com.jf.PetApp.infrastructure.entity.AcademySchoolTranslationJpaEntity;

@Repository
public interface AcademySchoolTranslationJpaRepository extends JpaRepository<AcademySchoolTranslationJpaEntity, Long> {

    List<AcademySchoolTranslationJpaEntity> findBySchoolId(String schoolId);

    List<AcademySchoolTranslationJpaEntity> findByLang(String lang);

    void deleteBySchoolId(String schoolId);

    Optional<AcademySchoolTranslationJpaEntity> findBySchoolIdAndLang(String schoolId, String lang);
}
