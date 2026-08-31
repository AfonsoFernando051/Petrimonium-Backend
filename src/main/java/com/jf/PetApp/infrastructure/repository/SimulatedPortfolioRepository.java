package com.jf.PetApp.infrastructure.repository;

import com.jf.PetApp.infrastructure.entity.SimulatedPortfolioJpaEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulatedPortfolioRepository extends JpaRepository<SimulatedPortfolioJpaEntity, Long> {
    Optional<SimulatedPortfolioJpaEntity> findByUser_Email(String email);
}
