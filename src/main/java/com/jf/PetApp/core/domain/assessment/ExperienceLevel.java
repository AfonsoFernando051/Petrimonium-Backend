package com.jf.PetApp.core.domain.assessment;

/**
 * The user's self-reported investing experience, chosen during Academy's
 * onboarding ("Como está sua experiência hoje?" step) — mirrors the Flutter
 * client's {@code ExperienceLevelEnum} one-to-one, serialized as this enum's
 * {@code name()}.
 */
public enum ExperienceLevel {
    NOVICE,
    CURIOUS,
    PRACTITIONER
}
