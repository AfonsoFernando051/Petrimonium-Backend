package com.jf.PetApp.application.lab.dto;

import java.util.Set;

public record SimulatorProgressResult(
        Set<String> completedSimulatorIds,
        int totalXp,
        int level,
        int xpIntoLevel,
        int xpForNextLevel) {
}
