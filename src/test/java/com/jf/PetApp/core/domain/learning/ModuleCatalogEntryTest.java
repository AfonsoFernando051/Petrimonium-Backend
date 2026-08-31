package com.jf.PetApp.core.domain.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * ModuleCatalogEntry is a plain record with no custom behavior — this
 * confirms construction and accessors.
 */
class ModuleCatalogEntryTest {

    @Test
    void accessors_ReturnConstructedValues() {
        ModuleCatalogEntry entry = new ModuleCatalogEntry("module-1", 100, 1, 5);

        assertEquals("module-1", entry.moduleId());
        assertEquals(100, entry.xpReward());
        assertEquals(1, entry.order());
        assertEquals(5, entry.lessonCount());
    }
}
