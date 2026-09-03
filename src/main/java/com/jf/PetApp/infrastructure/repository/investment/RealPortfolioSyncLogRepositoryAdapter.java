package com.jf.PetApp.infrastructure.repository.investment;

import com.jf.PetApp.application.investment.port.RealPortfolioSyncLogRepositoryPort;
import com.jf.PetApp.core.domain.RealPortfolioSyncLog;
import com.jf.PetApp.core.domain.enums.RealPortfolioSyncStatus;
import com.jf.PetApp.infrastructure.entity.RealPortfolioSyncLogJpaEntity;
import com.jf.PetApp.infrastructure.repository.RealPortfolioSyncLogRepository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public class RealPortfolioSyncLogRepositoryAdapter implements RealPortfolioSyncLogRepositoryPort {

    private static final int MESSAGE_MAX_LENGTH = 500;

    private final RealPortfolioSyncLogRepository repository;

    public RealPortfolioSyncLogRepositoryAdapter(RealPortfolioSyncLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RealPortfolioSyncLog> findByUserEmailAndProviderAndIdempotencyKey(
            String userEmail, String provider, String idempotencyKey) {
        return repository.findByUserEmailAndProviderAndIdempotencyKey(userEmail, provider, idempotencyKey)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public RealPortfolioSyncLog save(
            String userEmail,
            String provider,
            String idempotencyKey,
            RealPortfolioSyncStatus status,
            Instant startedAt,
            String message) {
        RealPortfolioSyncLogJpaEntity entity = new RealPortfolioSyncLogJpaEntity();
        entity.setUserEmail(userEmail);
        entity.setProvider(provider);
        entity.setIdempotencyKey(idempotencyKey);
        entity.setStatus(status);
        entity.setStartedAt(startedAt);
        entity.setFinishedAt(Instant.now());
        entity.setMessage(truncate(message));

        return toDomain(repository.save(entity));
    }

    /**
     * The {@code message} column is varchar(500) (V25); a raw provider exception message
     * can exceed that and would otherwise fail this insert — the one write every sync
     * attempt (including FAILED ones) must never lose.
     */
    private String truncate(String message) {
        if (message == null || message.length() <= MESSAGE_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, MESSAGE_MAX_LENGTH);
    }

    private RealPortfolioSyncLog toDomain(RealPortfolioSyncLogJpaEntity entity) {
        return new RealPortfolioSyncLog(
                entity.getId(),
                entity.getUserEmail(),
                entity.getProvider(),
                entity.getIdempotencyKey(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getMessage()
        );
    }
}
