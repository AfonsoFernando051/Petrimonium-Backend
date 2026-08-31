package com.jf.PetApp.infrastructure.seed.academy.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LessonSeedDtoTest {

    @Test
    void construction_RoundTripsFields() {
        Map<String, LessonTextDto> translations = Map.of("en", new LessonTextDto("Title", null));
        List<StepSeedDto> steps = List.of(new StepSeedDto("explanation", 1, null, null, Map.of()));

        List<String> portfolioConcepts = List.of("pe", "dy");

        LessonSeedDto dto = new LessonSeedDto(
                "lesson_1", 1, 50, "APPLY", 10,
                "PT", "2024-01-01", "2024-06-01", "tax_authority",
                portfolioConcepts, translations, steps);

        assertThat(dto.lessonId()).isEqualTo("lesson_1");
        assertThat(dto.order()).isEqualTo(1);
        assertThat(dto.xpReward()).isEqualTo(50);
        assertThat(dto.competency()).isEqualTo("APPLY");
        assertThat(dto.estimatedMinutes()).isEqualTo(10);
        assertThat(dto.jurisdiction()).isEqualTo("PT");
        assertThat(dto.effectiveDate()).isEqualTo("2024-01-01");
        assertThat(dto.lastVerifiedAt()).isEqualTo("2024-06-01");
        assertThat(dto.source()).isEqualTo("tax_authority");
        assertThat(dto.portfolioConcepts()).isEqualTo(portfolioConcepts);
        assertThat(dto.portfolioConceptsOrEmpty()).isEqualTo(portfolioConcepts);
        assertThat(dto.translations()).isEqualTo(translations);
        assertThat(dto.steps()).isEqualTo(steps);
    }

    @Test
    void construction_AllowsNullOptionalFields() {
        LessonSeedDto dto = new LessonSeedDto(
                "lesson_1", 1, 50, null, null,
                null, null, null, null, null,
                Map.of(), List.of());

        assertThat(dto.competency()).isNull();
        assertThat(dto.estimatedMinutes()).isNull();
        assertThat(dto.jurisdiction()).isNull();
        assertThat(dto.effectiveDate()).isNull();
        assertThat(dto.lastVerifiedAt()).isNull();
        assertThat(dto.source()).isNull();
        assertThat(dto.portfolioConcepts()).isNull();
        assertThat(dto.portfolioConceptsOrEmpty()).isEmpty();
    }
}
