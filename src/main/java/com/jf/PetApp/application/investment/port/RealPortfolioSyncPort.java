package com.jf.PetApp.application.investment.port;

import com.jf.PetApp.application.investment.dto.ExternalPositionDTO;

import java.util.List;

/**
 * A real brokerage/custody provider integration (e.g. B3) for syncing a
 * user's actual positions — deliberately never implemented against real B3
 * endpoints/SDKs/credentials in this codebase, since none exist yet
 * (docs/BACKEND_MODULE_PLAN.md §13). The only implementation today,
 * {@code B3RealPortfolioSyncAdapter}, always reports {@link #isEnabled()}
 * {@code false} and must never be called otherwise.
 */
public interface RealPortfolioSyncPort {

    /**
     * Whether this provider is actually configured (real credentials
     * present) and explicitly turned on. Callers must check this before
     * calling {@link #fetchPositions(String)} — never call it "to see what
     * happens".
     */
    boolean isEnabled();

    /** Display name for logs/audit rows, e.g. {@code "B3"}. */
    String providerName();

    /**
     * Fetches the user's current real positions from the provider.
     * Implementations must throw rather than fabricate a result if the
     * provider is unavailable or {@link #isEnabled()} is false.
     */
    List<ExternalPositionDTO> fetchPositions(String externalAccountReference);
}
