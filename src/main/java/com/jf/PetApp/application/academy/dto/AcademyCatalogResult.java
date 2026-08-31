package com.jf.PetApp.application.academy.dto;

import java.util.List;

/**
 * The full Academy curriculum, resolved to one language, as 4 flat lists
 * related to each other by id — mirroring the shape the Flutter client
 * already consumes today (no nested tree), so the client-side cutover to
 * this endpoint (see docs/DECISIONS.md) is a source swap, not a reshape.
 */
public record AcademyCatalogResult(
        List<AcademyDomainView> domains,
        List<AcademySchoolView> schools,
        List<AcademyModuleView> modules,
        List<AcademyLessonView> lessons) {
}
