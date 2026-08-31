package com.jf.PetApp.infrastructure.seed.academy.model;

import java.util.List;
import java.util.Map;

/** One school content JSON file (`academy-content/schools/{domainId}/{schoolId}.json`). */
public record SchoolSeedDto(
        String schoolId,
        String domainId,
        int order,
        String iconKey,
        boolean contentAvailable,
        List<String> prerequisites,
        Map<String, LocalizedTextDto> translations,
        List<ModuleSeedDto> modules) {
}
