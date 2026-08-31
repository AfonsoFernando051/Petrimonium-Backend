package com.jf.PetApp.infrastructure.entity;

import java.time.Instant;

import com.jf.PetApp.core.domain.RefreshToken;
import com.jf.PetApp.core.domain.enums.AppContextEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "jf_refresh_tokens", schema = "identity")
public class RefreshTokenJpaEntity {

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

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token_hash")
    private String replacedByTokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_context")
    private AppContextEnum appContext;

    public static RefreshTokenJpaEntity fromDomain(RefreshToken token) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.id = token.id();
        entity.userId = token.userId();
        entity.tokenHash = token.tokenHash();
        entity.expiresAt = token.expiresAt();
        entity.revokedAt = token.revokedAt();
        entity.replacedByTokenHash = token.replacedByTokenHash();
        entity.createdAt = token.createdAt();
        entity.appContext = token.appContext();
        return entity;
    }

    public RefreshToken toDomain() {
        return new RefreshToken(id, userId, tokenHash, expiresAt, revokedAt, replacedByTokenHash, createdAt, appContext);
    }

    /** Package-private mutator kept minimal on purpose — this entity is otherwise immutable
     * from the outside, only ever built via {@link #fromDomain(RefreshToken)}. */
    public void revoke(Instant revokedAt, String replacedByTokenHash) {
        this.revokedAt = revokedAt;
        this.replacedByTokenHash = replacedByTokenHash;
    }
}
