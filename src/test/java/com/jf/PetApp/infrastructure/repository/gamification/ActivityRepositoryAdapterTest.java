package com.jf.PetApp.infrastructure.repository.gamification;

import com.jf.PetApp.application.gamification.port.ActivityRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ActivityRepositoryAdapterTest {

    @Autowired
    private ActivityLogJpaRepository jpaRepository;

    private ActivityRepositoryPort adapter;

    @BeforeEach
    void setUp() {
        adapter = new ActivityRepositoryAdapter(jpaRepository);
    }

    @Test
    void recordActivity_ThenRecentActivityDates_IncludesIt() {
        LocalDate today = LocalDate.now();

        adapter.recordActivity(1L, today);

        assertThat(adapter.recentActivityDates(1L)).containsExactly(today);
    }

    @Test
    void recordActivity_CalledTwiceForSameDate_DoesNotCreateADuplicateRow() {
        LocalDate today = LocalDate.now();

        adapter.recordActivity(1L, today);
        adapter.recordActivity(1L, today);

        assertThat(adapter.recentActivityDates(1L)).hasSize(1);
    }

    @Test
    void recentActivityDates_ReturnsMostRecentFirst() {
        adapter.recordActivity(1L, LocalDate.now().minusDays(2));
        adapter.recordActivity(1L, LocalDate.now());
        adapter.recordActivity(1L, LocalDate.now().minusDays(1));

        List<LocalDate> dates = adapter.recentActivityDates(1L);

        assertThat(dates).containsExactly(
                LocalDate.now(), LocalDate.now().minusDays(1), LocalDate.now().minusDays(2));
    }

    @Test
    void recentActivityDates_IsolatedPerUser() {
        adapter.recordActivity(1L, LocalDate.now());
        adapter.recordActivity(2L, LocalDate.now());

        assertThat(adapter.recentActivityDates(1L)).hasSize(1);
    }
}
