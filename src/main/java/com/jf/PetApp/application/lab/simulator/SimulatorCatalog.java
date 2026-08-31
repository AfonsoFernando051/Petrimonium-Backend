package com.jf.PetApp.application.lab.simulator;

import java.util.List;
import java.util.Optional;

/**
 * The fixed, permanent Financial Lab simulator catalog (DECISION-037) — the
 * server-side source of truth for which simulator ids exist and how much XP
 * each is worth. The client never sends an XP amount; an unknown id is
 * rejected outright. Ids are the same stable snake_case strings the Flutter
 * app's {@code LabSimulatorId.sourceId} uses and the {@code xp_events}
 * ledger's idempotency key — never renamed once shipped, for the same
 * reason lesson/module ids never are.
 *
 * <p>Each reward sits at the bottom of {@code docs/FEATURES.md}'s sanctioned
 * "practice challenge" XP band (50-150) — a simulator is a sandbox
 * completion, not a graded lesson.
 */
public final class SimulatorCatalog {

    private SimulatorCatalog() {
    }

    public record SimulatorDefinition(String simulatorId, int xpReward) {
    }

    public static final List<SimulatorDefinition> DEFINITIONS = List.of(
            new SimulatorDefinition("compound_interest", 50),
            new SimulatorDefinition("inflation", 50),
            new SimulatorDefinition("fixed_income", 50),
            new SimulatorDefinition("diversification", 50),
            new SimulatorDefinition("portfolio", 50));

    public static Optional<SimulatorDefinition> find(String simulatorId) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.simulatorId().equals(simulatorId))
                .findFirst();
    }
}
