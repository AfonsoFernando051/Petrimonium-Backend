package com.jf.PetApp.infrastructure.repository;

import com.jf.PetApp.infrastructure.entity.RealPortfolioSyncLogJpaEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RealPortfolioSyncLogRepository extends JpaRepository<RealPortfolioSyncLogJpaEntity, Long> {
    Optional<RealPortfolioSyncLogJpaEntity> findByUserEmailAndProviderAndIdempotencyKey(
            String userEmail, String provider, String idempotencyKey);
}
