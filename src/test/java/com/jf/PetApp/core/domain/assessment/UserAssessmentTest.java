package com.jf.PetApp.core.domain.assessment;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Plain record with no validation annotations or custom mapping logic -- construction/accessor
 * test per this batch's exhaustive-coverage scope.
 */
class UserAssessmentTest {

    @Test
    void accessorsReturnConstructedValues() {
        UserAssessment assessment = new UserAssessment(42L, List.of("opt-1", "opt-2"));

        assertEquals(42L, assessment.userId());
        assertEquals(List.of("opt-1", "opt-2"), assessment.selectedOptionIds());
    }
}
