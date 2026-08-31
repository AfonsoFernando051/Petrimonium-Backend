package com.jf.PetApp.application.gamification.mission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissionContextTest {

    @Test
    void accessorsReturnConstructedValues() {
        MissionContext context = new MissionContext(3, 1);

        assertEquals(3, context.lessonsCompletedInPeriod());
        assertEquals(1, context.modulesCompletedInPeriod());
    }
}
