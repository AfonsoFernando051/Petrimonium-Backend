package com.jf.PetApp.infrastructure.repository.user;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jf.PetApp.infrastructure.entity.RefreshTokenJpaEntity;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    // clearAutomatically: a JPQL bulk update bypasses the persistence context, so without this
    // any token entity already managed in the current transaction (e.g. one just
    // findByTokenHash'd) would keep stale in-memory state even though the DB row changed
    // underneath it — same reasoning as PasswordResetTokenJpaRepository.
    @Modifying(clearAutomatically = true)
    @Query("update RefreshTokenJpaEntity t set t.revokedAt = :now, t.replacedByTokenHash = null "
            + "where t.userId = :userId and t.revokedAt is null")
    void revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
