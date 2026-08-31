package com.jf.PetApp.core.domain.assessment;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Plain record with no validation annotations or custom mapping logic -- construction/accessor
 * test per this batch's exhaustive-coverage scope.
 */
class QuestionTest {

    @Test
    void accessorsReturnConstructedValues() {
        Option option = new Option("opt-1", "Conservative", 1);
        Question question = new Question("q-1", "How do you feel about risk?", List.of(option));

        assertEquals("q-1", question.id());
        assertEquals("How do you feel about risk?", question.text());
        assertEquals(List.of(option), question.options());
    }
}
