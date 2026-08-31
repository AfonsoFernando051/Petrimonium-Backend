package com.jf.PetApp.infrastructure.seed.academy.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalizedTextDtoTest {

    @Test
    void construction_RoundTripsFields() {
        LocalizedTextDto dto = new LocalizedTextDto("Investing 101", "Learn the basics of investing.");

        assertThat(dto.title()).isEqualTo("Investing 101");
        assertThat(dto.description()).isEqualTo("Learn the basics of investing.");
    }
}
