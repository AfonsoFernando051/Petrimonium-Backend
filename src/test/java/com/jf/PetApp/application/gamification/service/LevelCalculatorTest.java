package com.jf.PetApp.application.gamification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.jf.PetApp.core.domain.gamification.PlayerLevel;

/**
 * Mirrors petapp_mobile/test/features/game/domain/services/level_calculator_test.dart
 * value-for-value, so the server and the Flutter client are provably in sync.
 */
class LevelCalculatorTest {

    @Test
    void zeroXpIsLevelOneWithZeroProgressIntoAFiftyXpWideLevel() {
        PlayerLevel result = LevelCalculator.fromXp(0);
        assertEquals(1, result.level());
        assertEquals(0, result.xpIntoLevel());
        assertEquals(50, result.xpForNextLevel());
    }

    @Test
    void justBelowTheLevelTwoThresholdStaysLevelOne() {
        PlayerLevel result = LevelCalculator.fromXp(49);
        assertEquals(1, result.level());
        assertEquals(49, result.xpIntoLevel());
    }

    @Test
    void exactlyAtTheLevelTwoThresholdAdvancesToLevelTwo() {
        PlayerLevel result = LevelCalculator.fromXp(50);
        assertEquals(2, result.level());
        assertEquals(0, result.xpIntoLevel());
        assertEquals(100, result.xpForNextLevel());
    }

    @Test
    void midWayThroughLevelTwo() {
        PlayerLevel result = LevelCalculator.fromXp(99);
        assertEquals(2, result.level());
        assertEquals(49, result.xpIntoLevel());
        assertEquals(100, result.xpForNextLevel());
    }

    @Test
    void exactlyAtTheLevelThreeThreshold() {
        PlayerLevel result = LevelCalculator.fromXp(150);
        assertEquals(3, result.level());
        assertEquals(0, result.xpIntoLevel());
        assertEquals(150, result.xpForNextLevel());
    }

    @Test
    void negativeXpIsNeverABugTreatedAsLevelOneZeroProgress() {
        PlayerLevel result = LevelCalculator.fromXp(-100);
        assertEquals(1, result.level());
        assertEquals(-100, result.xpIntoLevel());
    }

    @Test
    void levelRequirementsStrictlyIncrease() {
        int previousGap = 0;
        int xp = 0;
        for (int level = 1; level < 20; level++) {
            PlayerLevel atThreshold = LevelCalculator.fromXp(xp);
            int gap = atThreshold.xpForNextLevel();
            assertTrue(gap > previousGap, "level " + level + " requires more XP than the previous one");
            previousGap = gap;
            xp += gap;
        }
    }

    @Test
    void progressIsZeroExactlyAtALevelBoundary() {
        PlayerLevel result = LevelCalculator.fromXp(50);
        assertEquals(0.0, result.progress());
    }

    @Test
    void progressApproachesOneJustBeforeTheNextLevel() {
        PlayerLevel result = LevelCalculator.fromXp(149);
        assertEquals(0.99, result.progress(), 0.01);
    }
}
