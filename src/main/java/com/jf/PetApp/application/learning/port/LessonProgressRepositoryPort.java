package com.jf.PetApp.application.learning.port;

import java.util.Set;

import com.jf.PetApp.core.domain.learning.LessonProgressSnapshot;

/**
 * Application-layer boundary for per-user lesson completion state. Use
 * cases depend on this port, never on Spring Data or JPA entities directly.
 */
public interface LessonProgressRepositoryPort {

    boolean isLessonCompleted(Long userId, String lessonId);

    /**
     * Idempotent for completion (a lesson already marked completed stays
     * completed). {@code perfectFirstTry} is applied monotonically — see
     * DECISION-025: a replay that sets it {@code true} upgrades a
     * not-yet-perfect lesson, but {@code false} never downgrades an
     * already-perfect one.
     */
    void markCompleted(Long userId, String lessonId, boolean perfectFirstTry);

    Set<String> completedLessonIds(Long userId);

    /** Lesson ids the user has answered correctly on the first try at least once. */
    Set<String> perfectLessonIds(Long userId);

    /**
     * Both {@link #completedLessonIds(Long)} and {@link #perfectLessonIds(Long)}
     * in one fetch. Prefer this over calling both separately when a caller
     * needs both, since they're derived from the same underlying rows.
     */
    LessonProgressSnapshot snapshotFor(Long userId);
}
