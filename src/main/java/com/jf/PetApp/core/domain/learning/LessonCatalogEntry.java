package com.jf.PetApp.core.domain.learning;

/**
 * A lesson as known to the server: just enough to validate a completion
 * request and know how much XP it's worth. The actual lesson content
 * (explanations, questions) lives only in the Flutter app's static catalog.
 */
public record LessonCatalogEntry(String lessonId, String moduleId, int xpReward, int order) {
}
