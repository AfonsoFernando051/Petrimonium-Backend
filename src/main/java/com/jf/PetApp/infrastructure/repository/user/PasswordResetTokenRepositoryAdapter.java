package com.jf.PetApp.infrastructure.repository.user;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.auth.exception.PasswordResetTokenInvalidException;
import com.jf.PetApp.application.auth.port.PasswordResetTokenRepositoryPort;
import com.jf.PetApp.core.domain.PasswordResetToken;
import com.jf.PetApp.infrastructure.entity.PasswordResetTokenJpaEntity;

@Repository
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepositoryPort {

    private final PasswordResetTokenJpaRepository jpa;

    public PasswordResetTokenRepositoryAdapter(PasswordResetTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenJpaEntity entity = PasswordResetTokenJpaEntity.fromDomain(token);
        return jpa.save(entity).toDomain();
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(PasswordResetTokenJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public void invalidateOutstandingForUser(Long userId) {
        jpa.invalidateOutstandingForUser(userId, Instant.now());
    }

    @Override
    @Transactional
    public void markUsed(Long tokenId, Instant usedAt) {
        PasswordResetTokenJpaEntity entity = jpa.findById(tokenId)
                .orElseThrow(PasswordResetTokenInvalidException::new);
        entity.markUsed(usedAt);
        jpa.save(entity);
    }
}
