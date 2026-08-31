package com.jf.PetApp.application.investment.usecase;

import com.jf.PetApp.application.investment.dto.RealPortfolioSyncResultDTO;

public interface SyncRealPortfolioUseCase {
    RealPortfolioSyncResultDTO execute(String email, String externalAccountReference, String idempotencyKey);
}
