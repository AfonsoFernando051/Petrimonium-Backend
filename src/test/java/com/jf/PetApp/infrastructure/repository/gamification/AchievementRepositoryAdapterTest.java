package com.jf.PetApp.infrastructure.repository.gamification;

import com.jf.PetApp.application.gamification.port.AchievementRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AchievementRepositoryAdapterTest {

    @Autowired
    private AchievementUnlockJpaRepository jpaRepository;

    private AchievementRepositoryPort adapter;

    @BeforeEach
    void setUp() {
        adapter = new AchievementRepositoryAdapter(jpaRepository);
    }

    @Test
    void isUnlocked_WhenNeverUnlocked_ReturnsFalse() {
        assertThat(adapter.isUnlocked(1L, "FIRST_INVESTMENT")).isFalse();
    }

    @Test
    void unlock_ThenIsUnlocked_ReturnsTrue() {
        adapter.unlock(1L, "FIRST_INVESTMENT", 50);

        assertThat(adapter.isUnlocked(1L, "FIRST_INVESTMENT")).isTrue();
    }

    @Test
    void unlock_CalledTwiceForSameAchievement_IsIdempotentAndDoesNotDoubleAwardXp() {
        adapter.unlock(1L, "FIRST_INVESTMENT", 50);
        adapter.unlock(1L, "FIRST_INVESTMENT", 50);

        assertThat(adapter.totalXpFor(1L)).isEqualTo(50);
    }

    @Test
    void unlockedWithTimestamps_ReturnsEachUnlockedCodeMappedToWhenItUnlocked() {
        adapter.unlock(1L, "FIRST_INVESTMENT", 50);
        adapter.unlock(1L, "DIVERSIFIED", 30);

        Map<String, Instant> unlocked = adapter.unlockedWithTimestamps(1L);

        assertThat(unlocked).containsKeys("FIRST_INVESTMENT", "DIVERSIFIED");
        assertThat(unlocked.get("FIRST_INVESTMENT")).isNotNull();
    }

    @Test
    void totalXpFor_WithNoUnlocks_ReturnsZeroRatherThanNull() {
        assertThat(adapter.totalXpFor(999L)).isZero();
    }

    @Test
    void totalXpFor_SumsAcrossMultipleUnlockedAchievements() {
        adapter.unlock(1L, "FIRST_INVESTMENT", 50);
        adapter.unlock(1L, "DIVERSIFIED", 30);

        assertThat(adapter.totalXpFor(1L)).isEqualTo(80);
    }

    @Test
    void totalXpFor_IsolatedPerUser() {
        adapter.unlock(1L, "FIRST_INVESTMENT", 50);
        adapter.unlock(2L, "FIRST_INVESTMENT", 50);

        assertThat(adapter.totalXpFor(1L)).isEqualTo(50);
        assertThat(adapter.totalXpFor(2L)).isEqualTo(50);
    }
}
