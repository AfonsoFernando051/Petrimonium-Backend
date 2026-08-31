package com.jf.PetApp.infrastructure.repository.gamification;

import com.jf.PetApp.application.gamification.port.XpEventRepositoryPort;
import com.jf.PetApp.core.domain.gamification.XpEventType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class XpEventRepositoryAdapterTest {

    @Autowired
    private XpEventJpaRepository jpaRepository;

    private XpEventRepositoryPort adapter;

    @BeforeEach
    void setUp() {
        adapter = new XpEventRepositoryAdapter(jpaRepository);
    }

    @Test
    void existsByUserIdAndEventTypeAndSourceId_WhenNeverSaved_ReturnsFalse() {
        assertThat(adapter.existsByUserIdAndEventTypeAndSourceId(1L, XpEventType.LESSON_COMPLETED, "lesson1"))
                .isFalse();
    }

    @Test
    void save_ThenExistsByUserIdAndEventTypeAndSourceId_ReturnsTrue() {
        adapter.save(1L, XpEventType.LESSON_COMPLETED, 20, "lesson1");

        assertThat(adapter.existsByUserIdAndEventTypeAndSourceId(1L, XpEventType.LESSON_COMPLETED, "lesson1"))
                .isTrue();
    }

    @Test
    void existsByUserIdAndEventTypeAndSourceId_DoesNotMatchADifferentSourceId() {
        adapter.save(1L, XpEventType.LESSON_COMPLETED, 20, "lesson1");

        assertThat(adapter.existsByUserIdAndEventTypeAndSourceId(1L, XpEventType.LESSON_COMPLETED, "lesson2"))
                .isFalse();
    }

    @Test
    void sumAmountByUserId_WithNoEvents_ReturnsZeroRatherThanNull() {
        assertThat(adapter.sumAmountByUserId(999L)).isZero();
    }

    @Test
    void sumAmountByUserId_SumsAcrossMultipleEvents() {
        adapter.save(1L, XpEventType.LESSON_COMPLETED, 20, "lesson1");
        adapter.save(1L, XpEventType.MODULE_COMPLETED, 50, "module1");

        assertThat(adapter.sumAmountByUserId(1L)).isEqualTo(70);
    }

    @Test
    void sumAmountByUserId_IsolatedPerUser() {
        adapter.save(1L, XpEventType.LESSON_COMPLETED, 20, "lesson1");
        adapter.save(2L, XpEventType.LESSON_COMPLETED, 20, "lesson1");

        assertThat(adapter.sumAmountByUserId(1L)).isEqualTo(20);
        assertThat(adapter.sumAmountByUserId(2L)).isEqualTo(20);
    }

    @Test
    void countByUserIdAndEventTypeAndCreatedAtBetween_CountsOnlyMatchingEventTypeWithinTheWindow() {
        Instant now = Instant.now();
        adapter.save(1L, XpEventType.LESSON_COMPLETED, 20, "lesson1");
        adapter.save(1L, XpEventType.LESSON_COMPLETED, 20, "lesson2");
        adapter.save(1L, XpEventType.MODULE_COMPLETED, 50, "module1");

        int count = adapter.countByUserIdAndEventTypeAndCreatedAtBetween(
                1L, XpEventType.LESSON_COMPLETED, now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countByUserIdAndEventTypeAndCreatedAtBetween_ExcludesEventsOutsideTheWindow() {
        adapter.save(1L, XpEventType.LESSON_COMPLETED, 20, "lesson1");
        Instant future = Instant.now().plus(2, ChronoUnit.HOURS);

        int count = adapter.countByUserIdAndEventTypeAndCreatedAtBetween(
                1L, XpEventType.LESSON_COMPLETED, future, future.plus(1, ChronoUnit.HOURS));

        assertThat(count).isZero();
    }

    @Test
    void countByUserIdAndEventTypeAndCreatedAtBetween_IsolatedPerUser() {
        adapter.save(1L, XpEventType.LESSON_COMPLETED, 20, "lesson1");
        adapter.save(2L, XpEventType.LESSON_COMPLETED, 20, "lesson1");
        Instant now = Instant.now();

        int count = adapter.countByUserIdAndEventTypeAndCreatedAtBetween(
                1L, XpEventType.LESSON_COMPLETED, now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));

        assertThat(count).isEqualTo(1);
    }

    @Test
    void sourceIdsByUserIdAndEventType_ReturnsEverySourceIdForThatEventType() {
        adapter.save(1L, XpEventType.SIMULATOR_COMPLETED, 50, "compound_interest");
        adapter.save(1L, XpEventType.SIMULATOR_COMPLETED, 50, "inflation");
        adapter.save(1L, XpEventType.LESSON_COMPLETED, 20, "lesson1");

        assertThat(adapter.sourceIdsByUserIdAndEventType(1L, XpEventType.SIMULATOR_COMPLETED))
                .isEqualTo(Set.of("compound_interest", "inflation"));
    }

    @Test
    void sourceIdsByUserIdAndEventType_WithNoMatchingEvents_ReturnsEmptySet() {
        assertThat(adapter.sourceIdsByUserIdAndEventType(999L, XpEventType.SIMULATOR_COMPLETED)).isEmpty();
    }

    @Test
    void sourceIdsByUserIdAndEventType_IsolatedPerUser() {
        adapter.save(1L, XpEventType.SIMULATOR_COMPLETED, 50, "compound_interest");
        adapter.save(2L, XpEventType.SIMULATOR_COMPLETED, 50, "inflation");

        assertThat(adapter.sourceIdsByUserIdAndEventType(1L, XpEventType.SIMULATOR_COMPLETED))
                .isEqualTo(Set.of("compound_interest"));
    }
}
