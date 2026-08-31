package com.jf.PetApp.core.domain.learning;

import java.util.Set;

/**
 * A user's lesson completion state in one shot: every completed lesson id,
 * and the subset of those completed on a first-try perfect run. Fetched
 * together so callers needing both don't issue two separate queries over
 * the same underlying rows.
 */
public record LessonProgressSnapshot(Set<String> completedLessonIds, Set<String> perfectLessonIds) {
}
