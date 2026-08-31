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
        entity.setMessage(message);

        return toDomain(repository.save(entity));
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
