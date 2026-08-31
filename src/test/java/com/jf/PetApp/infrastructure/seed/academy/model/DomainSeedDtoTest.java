package com.jf.PetApp.infrastructure.seed.academy.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DomainSeedDtoTest {

    @Test
    void construction_RoundTripsFields() {
        Map<String, LocalizedTextDto> translations = Map.of("en", new LocalizedTextDto("Title", "Description"));

        DomainSeedDto dto = new DomainSeedDto("financial_education", 1, "domain_icon", translations);

        assertThat(dto.domainId()).isEqualTo("financial_education");
        assertThat(dto.order()).isEqualTo(1);
        assertThat(dto.iconKey()).isEqualTo("domain_icon");
        assertThat(dto.translations()).isEqualTo(translations);
    }
}
