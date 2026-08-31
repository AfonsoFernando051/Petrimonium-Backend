package com.jf.PetApp.infrastructure.repository.learning;

import com.jf.PetApp.application.learning.port.LessonProgressRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LessonProgressRepositoryAdapterTest {

    @Autowired
    private LessonProgressJpaRepository jpaRepository;

    private LessonProgressRepositoryPort adapter;

    @BeforeEach
    void setUp() {
        adapter = new LessonProgressRepositoryAdapter(jpaRepository);
    }

    @Test
    void isLessonCompleted_WhenNeverMarked_ReturnsFalse() {
        assertThat(adapter.isLessonCompleted(1L, "lesson1")).isFalse();
    }

    @Test
    void markCompleted_ThenIsLessonCompleted_ReturnsTrue() {
        adapter.markCompleted(1L, "lesson1", false);

        assertThat(adapter.isLessonCompleted(1L, "lesson1")).isTrue();
    }

    @Test
    void markCompleted_CalledTwice_DoesNotCreateADuplicateRow() {
        adapter.markCompleted(1L, "lesson1", false);
        adapter.markCompleted(1L, "lesson1", false);

        assertThat(adapter.completedLessonIds(1L)).hasSize(1);
    }

    @Test
    void completedLessonIds_ReturnsEveryCompletedLessonForThatUser() {
        adapter.markCompleted(1L, "lesson1", false);
        adapter.markCompleted(1L, "lesson2", false);

        assertThat(adapter.completedLessonIds(1L)).containsExactlyInAnyOrder("lesson1", "lesson2");
    }

    @Test
    void completedLessonIds_IsolatedPerUser() {
        adapter.markCompleted(1L, "lesson1", false);
        adapter.markCompleted(2L, "lesson1", false);

        assertThat(adapter.completedLessonIds(1L)).containsExactly("lesson1");
        assertThat(adapter.completedLessonIds(2L)).containsExactly("lesson1");
    }

    @Test
    void markCompleted_Perfect_AddsLessonToPerfectLessonIds() {
        adapter.markCompleted(1L, "lesson1", true);

        assertThat(adapter.perfectLessonIds(1L)).containsExactly("lesson1");
    }

    @Test
    void markCompleted_NotPerfect_DoesNotAddLessonToPerfectLessonIds() {
        adapter.markCompleted(1L, "lesson1", false);

        assertThat(adapter.perfectLessonIds(1L)).isEmpty();
    }

    @Test
    void markCompleted_PerfectReplayAfterImperfect_UpgradesToPerfect() {
        adapter.markCompleted(1L, "lesson1", false);
        adapter.markCompleted(1L, "lesson1", true);

        assertThat(adapter.perfectLessonIds(1L)).containsExactly("lesson1");
    }

    @Test
    void markCompleted_ImperfectReplayAfterPerfect_NeverDowngrades() {
        adapter.markCompleted(1L, "lesson1", true);
        adapter.markCompleted(1L, "lesson1", false);

        assertThat(adapter.perfectLessonIds(1L)).containsExactly("lesson1");
    }
}
