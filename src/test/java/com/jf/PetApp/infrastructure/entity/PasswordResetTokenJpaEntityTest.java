package com.jf.PetApp.infrastructure.entity;

import com.jf.PetApp.core.domain.PasswordResetToken;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokenJpaEntityTest {

    @Test
    void fromDomainThenToDomain_RoundTripsEveryField() {
        Instant now = Instant.now();
        PasswordResetToken token = new PasswordResetToken(
                1L, 2L, "hash", now.plusSeconds(1800), null, now);

        PasswordResetTokenJpaEntity entity = PasswordResetTokenJpaEntity.fromDomain(token);
        PasswordResetToken result = entity.toDomain();

        assertThat(result).isEqualTo(token);
    }

    @Test
    void markUsed_SetsUsedAtOnTheEntity() {
        Instant now = Instant.now();
        PasswordResetToken token = new PasswordResetToken(1L, 2L, "hash", now.plusSeconds(1800), null, now);
        PasswordResetTokenJpaEntity entity = PasswordResetTokenJpaEntity.fromDomain(token);

        Instant usedAt = now.plusSeconds(60);
        entity.markUsed(usedAt);

        assertThat(entity.toDomain().usedAt()).isEqualTo(usedAt);
    }
}
