package com.jf.PetApp.infrastructure.seed.academy.model;

import java.util.List;
import java.util.Map;

/**
 * One module from a school content JSON file, nested under a school.
 *
 * <p>{@code difficulty} (FOUNDATION, BEGINNER, INTERMEDIATE, ADVANCED, or
 * SPECIALIZATION) is optional — absent in most of today's authored content,
 * back-filled programmatically by {@code AcademyContentSeedRunner} from the
 * module's position in the prerequisite graph when not explicitly authored.
 * See DECISION-025.
 */
public record ModuleSeedDto(
        String moduleId,
        int order,
        String iconKey,
        int xpReward,
        boolean contentAvailable,
        String difficulty,
        List<String> prerequisites,
        Map<String, LocalizedTextDto> translations,
        List<LessonSeedDto> lessons) {
}
