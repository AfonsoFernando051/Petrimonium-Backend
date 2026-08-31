package com.jf.PetApp.core.domain.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Option is a plain record with no custom behavior — this confirms
 * construction and accessors.
 */
class OptionTest {

    @Test
    void accessors_ReturnConstructedValues() {
        Option option = new Option("opt-1", "Aggressive growth", 3);

        assertEquals("opt-1", option.id());
        assertEquals("Aggressive growth", option.text());
        assertEquals(3, option.points());
    }
}
