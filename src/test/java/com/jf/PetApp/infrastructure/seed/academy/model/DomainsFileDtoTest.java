package com.jf.PetApp.infrastructure.seed.academy.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DomainsFileDtoTest {

    @Test
    void construction_RoundTripsFields() {
        List<DomainSeedDto> domains = List.of(
                new DomainSeedDto("financial_education", 1, "domain_icon", Map.of()));

        DomainsFileDto dto = new DomainsFileDto(domains);

        assertThat(dto.domains()).isEqualTo(domains);
    }
}
