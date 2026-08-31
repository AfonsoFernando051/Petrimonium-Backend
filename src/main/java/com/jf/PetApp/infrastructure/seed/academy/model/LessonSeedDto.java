package com.jf.PetApp.infrastructure.seed.academy.model;

import java.util.List;
import java.util.Map;

/**
 * One lesson from a school content JSON file, nested under a module.
 *
 * <p>{@code competency} (one of the 8-level model: RECOGNIZE, EXPLAIN,
 * CALCULATE, INTERPRET, COMPARE, APPLY, DECIDE, INTEGRATE) and {@code
 * estimatedMinutes} are optional — absent in most of today's authored
 * content, back-filled programmatically by {@code AcademyContentSeedRunner}
 * from each lesson's step shape when not explicitly authored. {@code
 * jurisdiction}/{@code effectiveDate}/{@code lastVerifiedAt}/{@code source}
 * are Taxation-only regulatory metadata, {@code null} for every other
 * lesson — see DECISION-025.
 *
 * <p>{@code portfolioConcepts} is optional — {@code null}/absent for most
 * lessons, authored only where a lesson genuinely teaches a concept that
 * also has a real indicator id in the mobile client's
 * {@code IndicatorEducationCatalog} (e.g. {@code "pe"}, {@code "dy"},
 * {@code "roe"}), so the Educational Portfolio Intelligence callback on the
 * asset-details screen has something honest to point at. See DECISION-029.
 */
public record LessonSeedDto(
        String lessonId,
        int order,
        int xpReward,
        String competency,
        Integer estimatedMinutes,
        String jurisdiction,
        String effectiveDate,
        String lastVerifiedAt,
        String source,
        List<String> portfolioConcepts,
        Map<String, LessonTextDto> translations,
        List<StepSeedDto> steps) {

    /** {@code null}-safe accessor — most lessons author no {@code portfolioConcepts} at all. */
    public List<String> portfolioConceptsOrEmpty() {
        return portfolioConcepts == null ? List.of() : portfolioConcepts;
    }
}
