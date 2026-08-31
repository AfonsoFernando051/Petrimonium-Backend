package com.jf.PetApp.application.lab.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.jf.PetApp.application.lab.simulator.SimulatorCatalog.SimulatorDefinition;

class SimulatorCatalogTest {

    @Test
    void definitions_HasFiveEntries() {
        assertEquals(5, SimulatorCatalog.DEFINITIONS.size());
    }

    @Test
    void definitions_EveryIdIsUnique() {
        Set<String> ids = new HashSet<>();
        for (SimulatorDefinition definition : SimulatorCatalog.DEFINITIONS) {
            assertTrue(ids.add(definition.simulatorId()), "duplicate id: " + definition.simulatorId());
        }
    }

    @Test
    void definitions_EveryIdFitsTheSourceIdColumnBudget() {
        for (SimulatorDefinition definition : SimulatorCatalog.DEFINITIONS) {
            assertTrue(
                    definition.simulatorId().length() <= 64,
                    "id too long for xp_events.source_id varchar(64): " + definition.simulatorId());
        }
    }

    // docs/FEATURES.md's XP table sanctions 50-150 for a "practice challenge" —
    // a simulator completion is a sandbox activity, not a graded lesson, so it
    // should sit at or near the bottom of that band, never above it.
    @Test
    void definitions_EveryRewardIsWithinThePracticeChallengeBand() {
        for (SimulatorDefinition definition : SimulatorCatalog.DEFINITIONS) {
            assertTrue(definition.xpReward() >= 50 && definition.xpReward() <= 150);
        }
    }

    @Test
    void find_KnownId_ReturnsDefinition() {
        assertTrue(SimulatorCatalog.find("compound_interest").isPresent());
    }

    @Test
    void find_UnknownId_ReturnsEmpty() {
        assertFalse(SimulatorCatalog.find("not_a_simulator").isPresent());
    }
}
