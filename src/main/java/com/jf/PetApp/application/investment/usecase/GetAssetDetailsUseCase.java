package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.AssetDetailsResponseDTO;

/**
 * Fetches a comprehensive, enriched view of a single asset for the
 * authenticated user — market data, user position, dividends, and
 * metadata, all in one response. If the user doesn't own the asset,
 * {@code userPosition} will be {@code null} in the result.
 */
public interface GetAssetDetailsUseCase {

    AssetDetailsResponseDTO execute(String email, String ticker);
}
