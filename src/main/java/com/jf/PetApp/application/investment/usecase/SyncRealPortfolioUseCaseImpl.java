package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.ExternalPositionDTO;
import com.jf.PetApp.application.investment.dto.RealPortfolioSyncResultDTO;
import com.jf.PetApp.application.investment.port.RealPortfolioSyncLogRepositoryPort;
import com.jf.PetApp.application.investment.port.RealPortfolioSyncPort;
import com.jf.PetApp.core.domain.RealPortfolioSyncLog;
import com.jf.PetApp.core.domain.enums.RealPortfolioSyncStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates one real-portfolio sync attempt against {@link RealPortfolioSyncPort}
 * and always records the outcome via {@link RealPortfolioSyncLogRepositoryPort}
 * — idempotent per {@code (userEmail, provider, idempotencyKey)}, same shape
 * as {@code XpLedgerService}/{@code PlaceSimulatedOrderUseCaseImpl}.
 *
 * <p>Deliberately does not reconcile a successful fetch's
 * {@link ExternalPositionDTO} list into {@code jf_investments} — merging
 * external positions with a user's existing manually-entered lots (replace?
 * merge? flag conflicts?) is a real product decision that needs an actual
 * provider contract to design against, not something to guess at while
 * {@link RealPortfolioSyncPort} is permanently disabled. A successful fetch
 * is logged with the position count only.</p>
 */
@Service
public class SyncRealPortfolioUseCaseImpl implements SyncRealPortfolioUseCase {

    private static final Logger log = LoggerFactory.getLogger(SyncRealPortfolioUseCaseImpl.class);

    private final RealPortfolioSyncPort syncPort;
    private final RealPortfolioSyncLogRepositoryPort syncLogRepository;

    public SyncRealPortfolioUseCaseImpl(
            RealPortfolioSyncPort syncPort,
            RealPortfolioSyncLogRepositoryPort syncLogRepository) {
        this.syncPort = syncPort;
        this.syncLogRepository = syncLogRepository;
    }

    @Override
    public RealPortfolioSyncResultDTO execute(String email, String externalAccountReference, String idempotencyKey) {
        String provider = syncPort.providerName();
        String key = idempotencyKey != null && !idempotencyKey.isBlank()
                ? idempotencyKey.trim()
                : UUID.randomUUID().toString();

        Optional<RealPortfolioSyncLog> existing =
                syncLogRepository.findByUserEmailAndProviderAndIdempotencyKey(email, provider, key);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        Instant startedAt = Instant.now();

        if (!syncPort.isEnabled()) {
            RealPortfolioSyncLog logged = syncLogRepository.save(
                    email, provider, key, RealPortfolioSyncStatus.DISABLED, startedAt,
                    "No legitimate " + provider + " integration is configured yet.");
            return toDto(logged);
        }

        try {
            List<ExternalPositionDTO> positions = syncPort.fetchPositions(externalAccountReference);
            RealPortfolioSyncLog logged = syncLogRepository.save(
                    email, provider, key, RealPortfolioSyncStatus.COMPLETED, startedAt,
                    "Fetched " + positions.size() + " position(s) from " + provider + ".");
            return toDto(logged);
        } catch (Exception e) {
            log.warn("Real portfolio sync failed for provider {}: {}", provider, e.getMessage());
            RealPortfolioSyncLog logged = syncLogRepository.save(
                    email, provider, key, RealPortfolioSyncStatus.FAILED, startedAt,
                    "Sync failed: " + e.getMessage());
            return toDto(logged);
        }
    }

    private RealPortfolioSyncResultDTO toDto(RealPortfolioSyncLog logged) {
        return new RealPortfolioSyncResultDTO(
                logged.status().name(),
                logged.provider(),
                logged.message(),
                logged.startedAt(),
                logged.finishedAt()
        );
    }
}
