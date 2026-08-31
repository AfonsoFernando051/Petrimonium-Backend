package com.jf.PetApp.infrastructure.repository.user;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jf.PetApp.infrastructure.entity.PasswordResetTokenJpaEntity;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, Long> {

    Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

    // clearAutomatically: a JPQL bulk update bypasses the persistence context, so without this
    // any token entity already managed in the current transaction (e.g. one just findByTokenHash'd)
    // would keep stale in-memory state even though the DB row changed underneath it.
    @Modifying(clearAutomatically = true)
    @Query("update PasswordResetTokenJpaEntity t set t.usedAt = :now where t.userId = :userId and t.usedAt is null")
    void invalidateOutstandingForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
