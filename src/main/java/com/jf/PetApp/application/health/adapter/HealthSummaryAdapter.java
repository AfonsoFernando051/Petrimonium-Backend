package com.jf.PetApp.application.health.adapter;

import com.jf.PetApp.application.health.HealthService;
import com.jf.PetApp.application.mentor.port.HealthSummaryPort;
import com.jf.PetApp.core.domain.health.HealthModels.MonthlySummary;
import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapts {@link HealthService} to the Mentor's narrow {@link HealthSummaryPort}.
 *
 * <p>Lives on the Health side of the boundary on purpose: Health knows how to answer this question,
 * the Mentor only knows that it can ask it.
 */
@Component
public class HealthSummaryAdapter implements HealthSummaryPort {

    private final HealthService healthService;

    public HealthSummaryAdapter(HealthService healthService) {
        this.healthService = healthService;
    }

    @Override
    public Optional<MonthlySummary> currentMonthFor(String email) {
        try {
            // null month = current month, per HealthService.summary's own contract.
            return Optional.of(healthService.summary(email, null));
        } catch (ResourceNotFoundException e) {
            // No Health profile yet. The Mentor renders a "hasn't onboarded" context line rather
            // than failing the chat — the user can still talk to their companion.
            return Optional.empty();
        }
    }
}
