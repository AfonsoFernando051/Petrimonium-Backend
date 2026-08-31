package com.jf.PetApp.application.lab.dto;

public record SimulatorCompletionResult(
        String simulatorId,
        boolean alreadyCompleted,
        int xpAwarded,
        int totalXp,
        int level,
        int xpIntoLevel,
        int xpForNextLevel) {
}
