package com.jf.PetApp.infrastructure.seed.academy.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SchoolSeedDtoTest {

    @Test
    void construction_RoundTripsFields() {
        Map<String, LocalizedTextDto> translations = Map.of("en", new LocalizedTextDto("Title", "Description"));
        List<String> prerequisites = List.of("school_0");
        List<ModuleSeedDto> modules = List.of(
                new ModuleSeedDto("module_1", 1, "icon", 100, true, null, List.of(), Map.of(), List.of()));

        SchoolSeedDto dto = new SchoolSeedDto(
                "school_1", "domain_1", 1, "school_icon", true, prerequisites, translations, modules);

        assertThat(dto.schoolId()).isEqualTo("school_1");
        assertThat(dto.domainId()).isEqualTo("domain_1");
        assertThat(dto.order()).isEqualTo(1);
        assertThat(dto.iconKey()).isEqualTo("school_icon");
        assertThat(dto.contentAvailable()).isTrue();
        assertThat(dto.prerequisites()).isEqualTo(prerequisites);
        assertThat(dto.translations()).isEqualTo(translations);
        assertThat(dto.modules()).isEqualTo(modules);
    }
}
