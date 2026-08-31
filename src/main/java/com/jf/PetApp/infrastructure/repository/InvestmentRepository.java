package com.jf.PetApp.infrastructure.repository;

import com.jf.PetApp.infrastructure.entity.InvestmentJpaEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestmentRepository extends JpaRepository<InvestmentJpaEntity, Integer> {
    List<InvestmentJpaEntity> findByUser_Email(String email);
    
    @Modifying
    @Query("DELETE FROM InvestmentJpaEntity i WHERE i.user.email = :email")
    void deleteByUserEmail(String email);
}

