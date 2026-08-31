package com.jf.PetApp.application.gamification.dto;

public record GamificationSummaryResult(
        int totalXp, int level, int xpIntoLevel, int xpForNextLevel, int currentStreak, int longestStreak) {
}
