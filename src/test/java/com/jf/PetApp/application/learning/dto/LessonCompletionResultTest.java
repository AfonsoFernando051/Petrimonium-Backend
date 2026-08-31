package com.jf.PetApp.application.learning.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonCompletionResultTest {

    @Test
    void accessorsReturnConstructedValues() {
        LessonCompletionResult result = new LessonCompletionResult(
                "lesson-1", false, 10, true, 25, 150, 3, 50, 100);

        assertEquals("lesson-1", result.lessonId());
        assertFalse(result.alreadyCompleted());
        assertEquals(10, result.xpAwarded());
        assertTrue(result.moduleCompleted());
        assertEquals(25, result.moduleXpAwarded());
        assertEquals(150, result.totalXp());
        assertEquals(3, result.level());
        assertEquals(50, result.xpIntoLevel());
        assertEquals(100, result.xpForNextLevel());
    }

    @Test
    void accessorsReturnConstructedValues_WhenAlreadyCompleted() {
        LessonCompletionResult result = new LessonCompletionResult(
                "lesson-1", true, 0, false, 0, 150, 3, 50, 100);

        assertTrue(result.alreadyCompleted());
        assertEquals(0, result.xpAwarded());
        assertFalse(result.moduleCompleted());
    }
}
