package com.jf.PetApp.infrastructure.external;

import com.jf.PetApp.application.investment.dto.ExternalPositionDTO;
import com.jf.PetApp.application.investment.port.RealPortfolioSyncPort;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The only {@link RealPortfolioSyncPort} implementation in this codebase —
 * and, deliberately, a permanently-disabled one: there is no legitimate B3
 * (or any other brokerage) API contract, SDK, or credential in this
 * project. {@link #isEnabled()} requires both an explicit opt-in
 * ({@code app.b3-sync.enabled=true}) and a real token
 * ({@code api.b3.token} non-blank) — neither is set in any environment
 * today, so every call resolves to {@link #isEnabled()} {@code false} and
 * {@link #fetchPositions(String)} is never actually reached.
 *
 * <p>This class exists purely as the architectural seam: the day a real B3
 * integration is contracted, its HTTP client/mapping logic goes here
 * without any caller (SyncRealPortfolioUseCaseImpl, InvestmentController)
 * needing to change.</p>
 */
@Component
public class B3RealPortfolioSyncAdapter implements RealPortfolioSyncPort {

    @Value("${app.b3-sync.enabled:false}")
    private boolean syncEnabled;

    @Value("${api.b3.token:}")
    private String token;

    @Override
    public boolean isEnabled() {
        return syncEnabled && token != null && !token.isBlank();
    }

    @Override
    public String providerName() {
        return "B3";
    }

    @Override
    public List<ExternalPositionDTO> fetchPositions(String externalAccountReference) {
        if (!isEnabled()) {
            throw new IllegalStateException(
                    "B3RealPortfolioSyncAdapter is disabled — no legitimate B3 integration is configured. "
                    + "Callers must check isEnabled() before calling fetchPositions().");
        }
        // Unreachable until a real B3 contract, SDK, and credentials exist — see class doc.
        throw new UnsupportedOperationException("B3 integration is not implemented");
    }
}
