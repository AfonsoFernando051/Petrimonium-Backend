package com.jf.PetApp.core.domain.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/**
 * XpEvent is a plain record with no custom behavior — this confirms
 * construction of an immutable XP grant.
 */
class XpEventTest {

    @Test
    void accessors_ReturnConstructedValues() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        XpEvent event = new XpEvent(1L, 2L, XpEventType.LESSON_COMPLETED, 50, "lesson-1", createdAt);

        assertEquals(1L, event.id());
        assertEquals(2L, event.userId());
        assertEquals(XpEventType.LESSON_COMPLETED, event.eventType());
        assertEquals(50, event.amount());
        assertEquals("lesson-1", event.sourceId());
        assertEquals(createdAt, event.createdAt());
    }

    @Test
    void accessors_SupportModuleCompletedEventType() {
        XpEvent event = new XpEvent(1L, 2L, XpEventType.MODULE_COMPLETED, 100, "module-1", Instant.now());

        assertEquals(XpEventType.MODULE_COMPLETED, event.eventType());
    }
}
