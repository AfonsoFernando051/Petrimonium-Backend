package com.jf.PetApp.infrastructure.repository.user;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.auth.port.RefreshTokenRepositoryPort;
import com.jf.PetApp.core.domain.RefreshToken;
import com.jf.PetApp.infrastructure.entity.RefreshTokenJpaEntity;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.fromDomain(token);
        return jpa.save(entity).toDomain();
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(RefreshTokenJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public void revoke(Long tokenId, Instant revokedAt, String replacedByTokenHash) {
        jpa.findById(tokenId).ifPresent(entity -> {
            entity.revoke(revokedAt, replacedByTokenHash);
            jpa.save(entity);
        });
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId, Instant revokedAt) {
        jpa.revokeAllForUser(userId, revokedAt);
    }
}
