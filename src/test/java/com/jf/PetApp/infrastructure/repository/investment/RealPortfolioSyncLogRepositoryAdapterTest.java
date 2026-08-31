package com.jf.PetApp.infrastructure.repository.investment;

import com.jf.PetApp.application.investment.port.RealPortfolioSyncLogRepositoryPort;
import com.jf.PetApp.core.domain.RealPortfolioSyncLog;
import com.jf.PetApp.core.domain.enums.RealPortfolioSyncStatus;
import com.jf.PetApp.infrastructure.repository.RealPortfolioSyncLogRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RealPortfolioSyncLogRepositoryAdapterTest {

    @Autowired
    private RealPortfolioSyncLogRepository repository;

    private RealPortfolioSyncLogRepositoryPort adapter;

    private static final String EMAIL = "investor@test.com";

    @BeforeEach
    void setUp() {
        adapter = new RealPortfolioSyncLogRepositoryAdapter(repository);
    }

    @Test
    void save_ThenFindByUserEmailAndProviderAndIdempotencyKey_RoundTrips() {
        Instant startedAt = Instant.now();

        RealPortfolioSyncLog saved = adapter.save(
                EMAIL, "B3", "key-1", RealPortfolioSyncStatus.DISABLED, startedAt, "No integration configured");

        assertThat(saved.id()).isNotNull();
        assertThat(saved.userEmail()).isEqualTo(EMAIL);
        assertThat(saved.provider()).isEqualTo("B3");
        assertThat(saved.idempotencyKey()).isEqualTo("key-1");
        assertThat(saved.status()).isEqualTo(RealPortfolioSyncStatus.DISABLED);
        assertThat(saved.message()).isEqualTo("No integration configured");

        Optional<RealPortfolioSyncLog> found =
                adapter.findByUserEmailAndProviderAndIdempotencyKey(EMAIL, "B3", "key-1");
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(saved.id());
    }

    @Test
    void findByUserEmailAndProviderAndIdempotencyKey_WhenAbsent_ReturnsEmpty() {
        assertThat(adapter.findByUserEmailAndProviderAndIdempotencyKey(EMAIL, "B3", "does-not-exist")).isEmpty();
    }

    @Test
    void save_IsolatedPerIdempotencyKey() {
        adapter.save(EMAIL, "B3", "key-1", RealPortfolioSyncStatus.DISABLED, Instant.now(), "first");
        adapter.save(EMAIL, "B3", "key-2", RealPortfolioSyncStatus.DISABLED, Instant.now(), "second");

        assertThat(adapter.findByUserEmailAndProviderAndIdempotencyKey(EMAIL, "B3", "key-1").get().message())
                .isEqualTo("first");
        assertThat(adapter.findByUserEmailAndProviderAndIdempotencyKey(EMAIL, "B3", "key-2").get().message())
                .isEqualTo("second");
    }
}
