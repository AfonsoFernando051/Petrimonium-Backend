package com.jf.PetApp.infrastructure.repository.pet;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.core.domain.enums.AppContextEnum;
import com.jf.PetApp.infrastructure.entity.PetAppLinkJpaEntity;

@Repository
public interface PetAppLinkRepository extends JpaRepository<PetAppLinkJpaEntity, Integer> {

    Optional<PetAppLinkJpaEntity> findByUser_IdAndAppContext(Long userId, AppContextEnum appContext);

    void deleteByUser_Id(Long userId);
}
