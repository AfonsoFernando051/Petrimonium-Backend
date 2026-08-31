package com.jf.PetApp.infrastructure.repository;

import com.jf.PetApp.infrastructure.entity.SimulatedOrderJpaEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulatedOrderRepository extends JpaRepository<SimulatedOrderJpaEntity, Long> {
    List<SimulatedOrderJpaEntity> findByPortfolio_IdOrderByExecutedAtDesc(Long portfolioId);

    Optional<SimulatedOrderJpaEntity> findByPortfolio_IdAndClientOrderId(Long portfolioId, String clientOrderId);

    @Modifying
    @Query("DELETE FROM SimulatedOrderJpaEntity o WHERE o.portfolio.id = :portfolioId")
    void deleteByPortfolioId(Long portfolioId);
}
