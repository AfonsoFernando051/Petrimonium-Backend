package com.jf.PetApp.core.domain.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * LessonProgressSnapshot is a plain record with no custom behavior — this
 * confirms construction, including the empty-set case for a user with no
 * completions yet.
 */
class LessonProgressSnapshotTest {

    @Test
    void accessors_ReturnConstructedValues() {
        LessonProgressSnapshot snapshot = new LessonProgressSnapshot(
                Set.of("lesson-1", "lesson-2"), Set.of("lesson-1"));

        assertEquals(Set.of("lesson-1", "lesson-2"), snapshot.completedLessonIds());
        assertEquals(Set.of("lesson-1"), snapshot.perfectLessonIds());
        assertTrue(snapshot.completedLessonIds().containsAll(snapshot.perfectLessonIds()));
    }

    @Test
    void accessors_EmptySets_ForUserWithNoCompletions() {
        LessonProgressSnapshot snapshot = new LessonProgressSnapshot(Set.of(), Set.of());

        assertTrue(snapshot.completedLessonIds().isEmpty());
        assertTrue(snapshot.perfectLessonIds().isEmpty());
    }
}
