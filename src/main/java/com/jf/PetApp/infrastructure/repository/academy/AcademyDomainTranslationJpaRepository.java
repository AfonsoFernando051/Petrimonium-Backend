package com.jf.PetApp.infrastructure.repository.academy;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.AcademyDomainTranslationJpaEntity;

@Repository
public interface AcademyDomainTranslationJpaRepository extends JpaRepository<AcademyDomainTranslationJpaEntity, Long> {

    List<AcademyDomainTranslationJpaEntity> findByDomainId(String domainId);

    List<AcademyDomainTranslationJpaEntity> findByLang(String lang);

    Optional<AcademyDomainTranslationJpaEntity> findByDomainIdAndLang(
        String domainId,
        String lang
    );

    void deleteByDomainId(String domainId);
}
