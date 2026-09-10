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

import com.jf.PetApp.application.health.usecase.GetHealthProfileUseCase;
import com.jf.PetApp.application.health.usecase.SaveHealthProfileUseCase;

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
 * <p>This controller covers the user's country, primary currency and interface locale — the
 * prerequisite every other Health route checks for.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthProfileController {

    private final GetHealthProfileUseCase getHealthProfileUseCase;
    private final SaveHealthProfileUseCase saveHealthProfileUseCase;

    public HealthProfileController(
            GetHealthProfileUseCase getHealthProfileUseCase,
            SaveHealthProfileUseCase saveHealthProfileUseCase
    ) {
        this.getHealthProfileUseCase = getHealthProfileUseCase;
        this.saveHealthProfileUseCase = saveHealthProfileUseCase;
    }

    /**
     * 404 before onboarding: a Petrimonium account exists (the token proves it), but this user has
     * no Health profile yet, and the app reads that as "show onboarding" rather than inventing a
     * default country/currency for them.
     */
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile() {
        return getHealthProfileUseCase.execute(SecurityUtils.getCurrentUserEmail())
                .map(ProfileResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<ProfileResponse> saveProfile(@RequestBody ProfileRequest request) {
        return ResponseEntity.ok(ProfileResponse.from(
                saveHealthProfileUseCase.execute(SecurityUtils.getCurrentUserEmail(), request.toInput())));
    }
}
