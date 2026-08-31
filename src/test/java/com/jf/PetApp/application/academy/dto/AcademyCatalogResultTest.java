package com.jf.PetApp.application.academy.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AcademyCatalogResultTest {

    @Test
    void constructor_RoundTripsEachList() {
        List<AcademyDomainView> domains = List.of();
        List<AcademySchoolView> schools = List.of();
        List<AcademyModuleView> modules = List.of();
        List<AcademyLessonView> lessons = List.of();

        AcademyCatalogResult result = new AcademyCatalogResult(domains, schools, modules, lessons);

        assertThat(result.domains()).isSameAs(domains);
        assertThat(result.schools()).isSameAs(schools);
        assertThat(result.modules()).isSameAs(modules);
        assertThat(result.lessons()).isSameAs(lessons);
    }
}
