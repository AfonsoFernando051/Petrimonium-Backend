package com.jf.PetApp.application.investment.dto;

import java.time.Instant;

public record RealPortfolioSyncResultDTO(
        String status,
        String provider,
        String message,
        Instant startedAt,
        Instant finishedAt
) {
}
