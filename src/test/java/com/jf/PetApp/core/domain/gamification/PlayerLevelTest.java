package com.jf.PetApp.core.domain.gamification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayerLevelTest {

    @Test
    void progress_XpForNextLevelIsZero_ReturnsOneHundredPercent() {
        PlayerLevel level = new PlayerLevel(99, 0, 0);

        assertEquals(1.0, level.progress());
    }

    @Test
    void progress_HalfwayThroughLevel_ReturnsHalf() {
        PlayerLevel level = new PlayerLevel(2, 25, 50);

        assertEquals(0.5, level.progress());
    }

    @Test
    void progress_AtLevelStart_ReturnsZero() {
        PlayerLevel level = new PlayerLevel(2, 0, 50);

        assertEquals(0.0, level.progress());
    }

    @Test
    void progress_ExceedsNextLevelThreshold_ClampsToOne() {
        PlayerLevel level = new PlayerLevel(2, 75, 50);

        assertEquals(1.0, level.progress());
    }

    @Test
    void progress_NegativeXpIntoLevel_ClampsToZero() {
        PlayerLevel level = new PlayerLevel(2, -10, 50);

        assertEquals(0.0, level.progress());
    }

    @Test
    void accessors_ReturnConstructedValues() {
        PlayerLevel level = new PlayerLevel(3, 10, 100);

        assertEquals(3, level.level());
        assertEquals(10, level.xpIntoLevel());
        assertEquals(100, level.xpForNextLevel());
    }
}
