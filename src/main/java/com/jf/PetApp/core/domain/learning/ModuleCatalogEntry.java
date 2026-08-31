package com.jf.PetApp.core.domain.learning;

/**
 * A module as known to the server: its id, the bonus XP awarded once every
 * one of its lessons is completed, and how many lessons it has (used to
 * detect module completion without listing lesson ids twice).
 */
public record ModuleCatalogEntry(String moduleId, int xpReward, int order, int lessonCount) {
}
