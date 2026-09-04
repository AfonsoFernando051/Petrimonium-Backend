package com.jf.PetApp.application.mentor.port;

import com.jf.PetApp.core.domain.health.HealthModels.MonthlySummary;

import java.util.Optional;

/**
 * The only door through which the Mentor can see Health data.
 *
 * <p>Deliberately one read-only method returning one aggregate: the Mentor module does not depend
 * on {@code HealthService} (37 public methods, every mutation included), so there is no path by
 * which building a system prompt could write to the user's cash flow, and no temptation to widen
 * the Mentor's view of Health one getter at a time.
 */
public interface HealthSummaryPort {

    /**
     * The user's current month, or empty when they have no Health profile yet — a user who reached
     * the Mentor without finishing Health onboarding is a normal state, not an error.
     */
    Optional<MonthlySummary> currentMonthFor(String email);
}
