package com.jf.PetApp.infrastructure.repository.gamification;

import com.jf.PetApp.application.gamification.port.MissionRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MissionRepositoryAdapterTest {

    @Autowired
    private MissionCompletionJpaRepository jpaRepository;

    private MissionRepositoryPort adapter;

    @BeforeEach
    void setUp() {
        adapter = new MissionRepositoryAdapter(jpaRepository);
    }

    @Test
    void isCompleted_WhenNeverCompleted_ReturnsFalse() {
        assertThat(adapter.isCompleted(1L, "daily_complete_lesson", "2026-08-19")).isFalse();
    }

    @Test
    void complete_ThenIsCompleted_ReturnsTrue() {
        adapter.complete(1L, "daily_complete_lesson", "2026-08-19", 30);

        assertThat(adapter.isCompleted(1L, "daily_complete_lesson", "2026-08-19")).isTrue();
    }

    @Test
    void complete_CalledTwiceForSamePeriod_IsIdempotentAndDoesNotDoubleAwardXp() {
        adapter.complete(1L, "daily_complete_lesson", "2026-08-19", 30);
        adapter.complete(1L, "daily_complete_lesson", "2026-08-19", 30);

        assertThat(adapter.totalXpFor(1L)).isEqualTo(30);
    }

    @Test
    void complete_ANewPeriodForTheSameMission_IsANewInstance() {
        adapter.complete(1L, "daily_complete_lesson", "2026-08-19", 30);
        adapter.complete(1L, "daily_complete_lesson", "2026-08-20", 30);

        assertThat(adapter.isCompleted(1L, "daily_complete_lesson", "2026-08-19")).isTrue();
        assertThat(adapter.isCompleted(1L, "daily_complete_lesson", "2026-08-20")).isTrue();
        assertThat(adapter.totalXpFor(1L)).isEqualTo(60);
    }

    @Test
    void totalXpFor_WithNoCompletions_ReturnsZeroRatherThanNull() {
        assertThat(adapter.totalXpFor(999L)).isZero();
    }

    @Test
    void totalXpFor_SumsAcrossMultipleCompletedMissions() {
        adapter.complete(1L, "daily_complete_lesson", "2026-08-19", 30);
        adapter.complete(1L, "weekly_complete_module", "2026-W34", 150);

        assertThat(adapter.totalXpFor(1L)).isEqualTo(180);
    }

    @Test
    void totalXpFor_IsolatedPerUser() {
        adapter.complete(1L, "daily_complete_lesson", "2026-08-19", 30);
        adapter.complete(2L, "daily_complete_lesson", "2026-08-19", 30);

        assertThat(adapter.totalXpFor(1L)).isEqualTo(30);
        assertThat(adapter.totalXpFor(2L)).isEqualTo(30);
    }
}
