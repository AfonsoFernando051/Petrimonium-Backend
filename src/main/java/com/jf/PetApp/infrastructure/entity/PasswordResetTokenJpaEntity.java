package com.jf.PetApp.infrastructure.entity;

import java.time.Instant;

import com.jf.PetApp.core.domain.PasswordResetToken;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "jf_password_reset_tokens", schema = "identity")
public class PasswordResetTokenJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static PasswordResetTokenJpaEntity fromDomain(PasswordResetToken token) {
        PasswordResetTokenJpaEntity entity = new PasswordResetTokenJpaEntity();
        entity.id = token.id();
        entity.userId = token.userId();
        entity.tokenHash = token.tokenHash();
        entity.expiresAt = token.expiresAt();
        entity.usedAt = token.usedAt();
        entity.createdAt = token.createdAt();
        return entity;
    }

    public PasswordResetToken toDomain() {
        return new PasswordResetToken(id, userId, tokenHash, expiresAt, usedAt, createdAt);
    }

    /** Package-private mutator kept minimal on purpose — this entity is otherwise immutable
     * from the outside, only ever built via {@link #fromDomain(PasswordResetToken)}. */
    public void markUsed(Instant usedAt) {
        this.usedAt = usedAt;
    }
}
