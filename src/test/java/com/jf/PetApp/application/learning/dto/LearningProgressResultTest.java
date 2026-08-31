package com.jf.PetApp.application.learning.dto;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LearningProgressResultTest {

    @Test
    void accessorsReturnConstructedValues() {
        LearningProgressResult result = new LearningProgressResult(
                Set.of("lesson-1"), Set.of("module-1"), Set.of("lesson-1"), 150, 3, 50, 100);

        assertEquals(Set.of("lesson-1"), result.completedLessonIds());
        assertEquals(Set.of("module-1"), result.completedModuleIds());
        assertEquals(Set.of("lesson-1"), result.perfectLessonIds());
        assertEquals(150, result.totalXp());
        assertEquals(3, result.level());
        assertEquals(50, result.xpIntoLevel());
        assertEquals(100, result.xpForNextLevel());
    }
}
