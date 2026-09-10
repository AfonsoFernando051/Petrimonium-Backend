package com.jf.PetApp.infrastructure.repository;

import com.jf.PetApp.infrastructure.entity.InvestmentJpaEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestmentRepository extends JpaRepository<InvestmentJpaEntity, Integer> {
    List<InvestmentJpaEntity> findByUser_Email(String email);

    /** Scoped by owner on purpose — see {@code InvestmentRepositoryAdapter#ownedOrThrow}. */
    Optional<InvestmentJpaEntity> findByIdAndUser_Email(Integer id, String email);

    @Modifying
    @Query("DELETE FROM InvestmentJpaEntity i WHERE i.user.email = :email")
    void deleteByUserEmail(String email);
}

