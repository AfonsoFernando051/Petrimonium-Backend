package com.jf.PetApp.application.gamification.achievement;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AchievementDefinitionTest {

    private static AchievementContext contextWith(boolean hasHoldings) {
        return new AchievementContext(hasHoldings, 0.0, 0.0, 0, 0, LocalDate.now(), 0.0, 0.0);
    }

    @Test
    void accessorsReturnConstructedValues() {
        Predicate<AchievementContext> qualifies = AchievementContext::hasHoldings;
        AchievementDefinition definition = new AchievementDefinition("FIRST_ASSET", 100, qualifies);

        assertEquals("FIRST_ASSET", definition.code());
        assertEquals(100, definition.xpReward());
        assertEquals(qualifies, definition.qualifies());
    }

    @Test
    void qualifies_WhenTheContextSatisfiesThePredicate_ReturnsTrue() {
        AchievementDefinition definition =
                new AchievementDefinition("FIRST_ASSET", 100, AchievementContext::hasHoldings);

        assertTrue(definition.qualifies().test(contextWith(true)));
    }

    @Test
    void qualifies_WhenTheContextDoesNotSatisfyThePredicate_ReturnsFalse() {
        AchievementDefinition definition =
                new AchievementDefinition("FIRST_ASSET", 100, AchievementContext::hasHoldings);

        assertFalse(definition.qualifies().test(contextWith(false)));
    }
}
