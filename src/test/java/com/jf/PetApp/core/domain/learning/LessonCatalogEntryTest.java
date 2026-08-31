package com.jf.PetApp.core.domain.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * LessonCatalogEntry is a plain record with no custom behavior — this
 * confirms construction and accessors.
 */
class LessonCatalogEntryTest {

    @Test
    void accessors_ReturnConstructedValues() {
        LessonCatalogEntry entry = new LessonCatalogEntry("lesson-1", "module-1", 25, 1);

        assertEquals("lesson-1", entry.lessonId());
        assertEquals("module-1", entry.moduleId());
        assertEquals(25, entry.xpReward());
        assertEquals(1, entry.order());
    }
}
