package com.jf.PetApp.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.PetJpaEntity;

@Repository
public interface PetRepository extends JpaRepository<PetJpaEntity, Integer> {
}
