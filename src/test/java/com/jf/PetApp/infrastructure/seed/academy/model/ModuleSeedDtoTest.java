package com.jf.PetApp.infrastructure.seed.academy.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleSeedDtoTest {

    @Test
    void construction_RoundTripsFields() {
        Map<String, LocalizedTextDto> translations = Map.of("en", new LocalizedTextDto("Title", "Description"));
        List<String> prerequisites = List.of("module_0");
        List<LessonSeedDto> lessons = List.of(
                new LessonSeedDto("lesson_1", 1, 50, null, null, null, null, null, null, null, Map.of(), List.of()));

        ModuleSeedDto dto = new ModuleSeedDto(
                "module_1", 1, "module_icon", 100, true, "FOUNDATION",
                prerequisites, translations, lessons);

        assertThat(dto.moduleId()).isEqualTo("module_1");
        assertThat(dto.order()).isEqualTo(1);
        assertThat(dto.iconKey()).isEqualTo("module_icon");
        assertThat(dto.xpReward()).isEqualTo(100);
        assertThat(dto.contentAvailable()).isTrue();
        assertThat(dto.difficulty()).isEqualTo("FOUNDATION");
        assertThat(dto.prerequisites()).isEqualTo(prerequisites);
        assertThat(dto.translations()).isEqualTo(translations);
        assertThat(dto.lessons()).isEqualTo(lessons);
    }

    @Test
    void construction_AllowsNullDifficulty() {
        ModuleSeedDto dto = new ModuleSeedDto(
                "module_1", 1, "module_icon", 100, false, null, List.of(), Map.of(), List.of());

        assertThat(dto.difficulty()).isNull();
        assertThat(dto.contentAvailable()).isFalse();
    }
}
