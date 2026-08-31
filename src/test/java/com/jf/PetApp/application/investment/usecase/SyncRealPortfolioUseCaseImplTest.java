package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.ExternalPositionDTO;
import com.jf.PetApp.application.investment.dto.RealPortfolioSyncResultDTO;
import com.jf.PetApp.application.investment.port.RealPortfolioSyncLogRepositoryPort;
import com.jf.PetApp.application.investment.port.RealPortfolioSyncPort;
import com.jf.PetApp.core.domain.RealPortfolioSyncLog;
import com.jf.PetApp.core.domain.enums.RealPortfolioSyncStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncRealPortfolioUseCaseImplTest {

    @Mock
    private RealPortfolioSyncPort syncPort;

    @Mock
    private RealPortfolioSyncLogRepositoryPort syncLogRepository;

    private SyncRealPortfolioUseCaseImpl useCase;

    private static final String EMAIL = "investor@test.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new SyncRealPortfolioUseCaseImpl(syncPort, syncLogRepository);
        when(syncPort.providerName()).thenReturn("B3");
    }

    private RealPortfolioSyncLog loggedResult(RealPortfolioSyncStatus status, String message) {
        return new RealPortfolioSyncLog(1L, EMAIL, "B3", "key-1", status, Instant.now(), Instant.now(), message);
    }

    @Test
    void execute_WhenPortDisabled_LogsDisabledAndNeverCallsFetchPositions() {
        when(syncPort.isEnabled()).thenReturn(false);
        when(syncLogRepository.findByUserEmailAndProviderAndIdempotencyKey(eq(EMAIL), eq("B3"), any()))
                .thenReturn(Optional.empty());
        when(syncLogRepository.save(eq(EMAIL), eq("B3"), any(), eq(RealPortfolioSyncStatus.DISABLED), any(), any()))
                .thenReturn(loggedResult(RealPortfolioSyncStatus.DISABLED, "No legitimate B3 integration is configured yet."));

        RealPortfolioSyncResultDTO result = useCase.execute(EMAIL, null, null);

        assertEquals("DISABLED", result.status());
        assertEquals("B3", result.provider());
        verify(syncPort, never()).fetchPositions(any());
    }

    @Test
    void execute_WithExistingLogForTheSameIdempotencyKey_ReturnsItWithoutReRunning() {
        RealPortfolioSyncLog existing = loggedResult(RealPortfolioSyncStatus.COMPLETED, "Fetched 3 position(s) from B3.");
        when(syncLogRepository.findByUserEmailAndProviderAndIdempotencyKey(EMAIL, "B3", "retry-key"))
                .thenReturn(Optional.of(existing));

        RealPortfolioSyncResultDTO result = useCase.execute(EMAIL, null, "retry-key");

        assertEquals("COMPLETED", result.status());
        verify(syncPort, never()).isEnabled();
        verify(syncPort, never()).fetchPositions(any());
        verify(syncLogRepository, never()).save(any(), any(), any(), any(), any(), any());
    }

    @Test
    void execute_WhenPortEnabledAndFetchSucceeds_LogsCompletedWithPositionCount() {
        when(syncPort.isEnabled()).thenReturn(true);
        when(syncLogRepository.findByUserEmailAndProviderAndIdempotencyKey(eq(EMAIL), eq("B3"), any()))
                .thenReturn(Optional.empty());
        when(syncPort.fetchPositions("account-123")).thenReturn(List.of(
                new ExternalPositionDTO("PETR4", BigDecimal.TEN, BigDecimal.valueOf(30), LocalDate.now())
        ));
        when(syncLogRepository.save(eq(EMAIL), eq("B3"), any(), eq(RealPortfolioSyncStatus.COMPLETED), any(), any()))
                .thenReturn(loggedResult(RealPortfolioSyncStatus.COMPLETED, "Fetched 1 position(s) from B3."));

        RealPortfolioSyncResultDTO result = useCase.execute(EMAIL, "account-123", null);

        assertEquals("COMPLETED", result.status());
        verify(syncLogRepository).save(eq(EMAIL), eq("B3"), any(), eq(RealPortfolioSyncStatus.COMPLETED), any(),
                eq("Fetched 1 position(s) from B3."));
    }

    @Test
    void execute_WhenPortEnabledAndFetchThrows_LogsFailedWithoutPropagating() {
        when(syncPort.isEnabled()).thenReturn(true);
        when(syncLogRepository.findByUserEmailAndProviderAndIdempotencyKey(eq(EMAIL), eq("B3"), any()))
                .thenReturn(Optional.empty());
        when(syncPort.fetchPositions(any())).thenThrow(new RuntimeException("provider down"));
        when(syncLogRepository.save(eq(EMAIL), eq("B3"), any(), eq(RealPortfolioSyncStatus.FAILED), any(), any()))
                .thenReturn(loggedResult(RealPortfolioSyncStatus.FAILED, "Sync failed: provider down"));

        RealPortfolioSyncResultDTO result = useCase.execute(EMAIL, null, null);

        assertEquals("FAILED", result.status());
    }

    @Test
    void execute_WithoutClientIdempotencyKey_GeneratesOneBeforeLogging() {
        when(syncPort.isEnabled()).thenReturn(false);
        when(syncLogRepository.findByUserEmailAndProviderAndIdempotencyKey(eq(EMAIL), eq("B3"), any()))
                .thenReturn(Optional.empty());
        when(syncLogRepository.save(any(), any(), any(), any(), any(), any()))
                .thenReturn(loggedResult(RealPortfolioSyncStatus.DISABLED, "x"));

        useCase.execute(EMAIL, null, null);

        verify(syncLogRepository).findByUserEmailAndProviderAndIdempotencyKey(eq(EMAIL), eq("B3"),
                org.mockito.ArgumentMatchers.argThat(key -> key != null && key.length() == 36));
    }
}
