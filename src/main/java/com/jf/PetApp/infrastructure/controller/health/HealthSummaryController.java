package com.jf.PetApp.infrastructure.controller.health;

import static com.jf.PetApp.application.health.dto.HealthCommands.*;

import com.jf.PetApp.core.security.SecurityUtils;
import com.jf.PetApp.infrastructure.controller.health.dto.HealthApiDtos.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.jf.PetApp.application.health.usecase.GetHealthSummaryUseCase;

/**
 * Health-only (see SecurityConfig: {@code hasAuthority(APP_CONTEXT_HEALTH)} on the whole
 * {@code /api/v1/health} path). This is the user's real money — salary, rent, card invoices — and
 * is never reachable from an Academy session (whose portfolio is simulated) or a Wallet one.
 *
 * <p>The owner is always {@link SecurityUtils#getCurrentUserEmail()}, resolved from the JWT
 * subject: no route accepts a user id, and every id in a path is checked against that owner
 * inside the Health use cases, which answers 404 rather than 403 for someone else's row so a
 * probe cannot confirm that the row exists.
 *
 * <p>Validation lives in the Health use cases rather than in bean-validation annotations here,
 * so a rule ("at most two decimal places", "the currency must match the profile") holds for every
 * caller and produces one stable error code, not two depending on which layer noticed first.
 *
 * <p>Nothing here logs a body, an amount, a balance or a description; the Health request log is
 * the shared {@code RequestIdFilter} correlation id and the status code, nothing more.
 *
 * <p>This controller covers the month's rolled-up view: balances, totals, categories and what is
 * still due.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthSummaryController {

    private final GetHealthSummaryUseCase getHealthSummaryUseCase;

    public HealthSummaryController(
            GetHealthSummaryUseCase getHealthSummaryUseCase
    ) {
        this.getHealthSummaryUseCase = getHealthSummaryUseCase;
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryResponse> summary(@RequestParam(required = false) String month) {
        YearMonth parsed = parseMonth(month);
        return ResponseEntity.ok(SummaryResponse.from(
                getHealthSummaryUseCase.execute(SecurityUtils.getCurrentUserEmail(), parsed)));
    }

    private static YearMonth parseMonth(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(raw.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("month must be formatted as YYYY-MM");
        }
    }
}
