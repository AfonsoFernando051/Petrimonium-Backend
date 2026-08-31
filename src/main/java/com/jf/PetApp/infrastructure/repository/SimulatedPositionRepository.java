package com.jf.PetApp.infrastructure.repository;

import com.jf.PetApp.infrastructure.entity.SimulatedPositionJpaEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulatedPositionRepository extends JpaRepository<SimulatedPositionJpaEntity, Long> {
    List<SimulatedPositionJpaEntity> findByPortfolio_IdOrderByTicker(Long portfolioId);

    Optional<SimulatedPositionJpaEntity> findByPortfolio_IdAndTicker(Long portfolioId, String ticker);

    @Modifying
    @Query("DELETE FROM SimulatedPositionJpaEntity p WHERE p.portfolio.id = :portfolioId AND p.ticker = :ticker")
    void deleteByPortfolioIdAndTicker(Long portfolioId, String ticker);

    @Modifying
    @Query("DELETE FROM SimulatedPositionJpaEntity p WHERE p.portfolio.id = :portfolioId")
    void deleteByPortfolioId(Long portfolioId);
}
