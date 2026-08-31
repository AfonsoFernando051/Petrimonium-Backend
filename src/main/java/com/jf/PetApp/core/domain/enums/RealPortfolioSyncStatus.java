package com.jf.PetApp.core.domain.enums;

public enum RealPortfolioSyncStatus {
    /** No real provider integration is configured/enabled — the expected
     * outcome in every environment today (no legitimate B3 credentials
     * exist). Never an error; a normal, user-facing "not yet available". */
    DISABLED,
    COMPLETED,
    FAILED
}
