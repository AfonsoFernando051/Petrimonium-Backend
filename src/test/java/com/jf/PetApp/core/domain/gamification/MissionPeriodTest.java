package com.jf.PetApp.core.domain.gamification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link MissionPeriod} is a bare enum with no logic of its own -- the real period-key/date
 * behavior lives in {@code MissionPeriodKeyCalculator} (see
 * {@code MissionPeriodKeyCalculatorTest}), so this is a minimal constants smoke test.
 */
class MissionPeriodTest {

    @Test
    void values_ReturnsBothPeriodsInDeclarationOrder() {
        assertArrayEquals(new MissionPeriod[] {MissionPeriod.DAILY, MissionPeriod.WEEKLY}, MissionPeriod.values());
    }

    @Test
    void valueOf_WithEachConstantName_ReturnsTheMatchingConstant() {
        assertEquals(MissionPeriod.DAILY, MissionPeriod.valueOf("DAILY"));
        assertEquals(MissionPeriod.WEEKLY, MissionPeriod.valueOf("WEEKLY"));
    }
}
