package com.jf.PetApp.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jf.PetApp.infrastructure.entity.PetJpaEntity;

@Repository
public interface PetRepository extends JpaRepository<PetJpaEntity, Integer> {

    List<PetJpaEntity> findAllByUser_Id(Long userId);

    void deleteByUser_Id(Long userId);
}
