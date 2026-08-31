package com.jf.PetApp.application.investment.dto;

import java.util.List;

/**
 * Wire shape for {@code GET /api/investments/dividends}: the user's real
 * holdings' confirmed corporate-action history, split into what's still
 * ahead ({@code upcoming}, announced but not yet paid) and what has already
 * been paid ({@code history}, most recent first).
 */
public record DividendRadarResponseDTO(
    List<DividendRadarEntryDTO> upcoming,
    List<DividendRadarEntryDTO> history
) {
}
