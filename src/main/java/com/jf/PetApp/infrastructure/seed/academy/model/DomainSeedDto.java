package com.jf.PetApp.infrastructure.seed.academy.model;

import java.util.Map;

/** One entry of `academy-content/domains.json`. */
public record DomainSeedDto(String domainId, int order, String iconKey, Map<String, LocalizedTextDto> translations) {
}
