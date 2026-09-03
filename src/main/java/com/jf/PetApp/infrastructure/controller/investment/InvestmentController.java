package com.jf.PetApp.infrastructure.controller.investment;

import com.jf.PetApp.application.investment.usecase.ConfigureInvestmentsUseCase;
import com.jf.PetApp.application.investment.usecase.GetAssetDetailsUseCase;
import com.jf.PetApp.application.investment.usecase.GetDividendRadarUseCase;
import com.jf.PetApp.application.investment.usecase.GetPortfolioAllocationUseCase;
import com.jf.PetApp.application.investment.usecase.GetPortfolioHistoryUseCase;
import com.jf.PetApp.application.investment.usecase.GetPortfolioHoldingsUseCase;
import com.jf.PetApp.application.investment.usecase.GetPortfolioSummaryUseCase;
import com.jf.PetApp.application.investment.usecase.ConfigureInvestmentCommand;
import com.jf.PetApp.application.investment.usecase.SyncRealPortfolioUseCase;
import com.jf.PetApp.core.security.SecurityUtils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.jf.PetApp.infrastructure.controller.investment.dto.AssetRegistrationDto;
import com.jf.PetApp.infrastructure.controller.investment.dto.SyncRealPortfolioRequest;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;
import com.jf.PetApp.application.investment.dto.AssetDetailsResponseDTO;
import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.dto.InvestmentLotDTO;
import com.jf.PetApp.application.investment.dto.PortfolioSummaryDTO;
import com.jf.PetApp.application.investment.dto.AllocationSliceDTO;
import com.jf.PetApp.application.investment.dto.PortfolioHistoryPointDTO;
import com.jf.PetApp.application.investment.dto.DividendRadarResponseDTO;
import com.jf.PetApp.application.investment.dto.RealPortfolioSyncResultDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final ConfigureInvestmentsUseCase configureInvestmentsUseCase;
    private final ExternalInvestmentApiPort externalInvestmentApiPort;
    private final GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase;
    private final GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;
    private final GetPortfolioAllocationUseCase getPortfolioAllocationUseCase;
    private final GetPortfolioHistoryUseCase getPortfolioHistoryUseCase;
    private final GetDividendRadarUseCase getDividendRadarUseCase;
    private final GetAssetDetailsUseCase getAssetDetailsUseCase;
    private final SyncRealPortfolioUseCase syncRealPortfolioUseCase;
    private final Validator validator;

    public InvestmentController(ConfigureInvestmentsUseCase configureInvestmentsUseCase,
                                 ExternalInvestmentApiPort externalInvestmentApiPort,
                                 GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase,
                                 GetPortfolioSummaryUseCase getPortfolioSummaryUseCase,
                                 GetPortfolioAllocationUseCase getPortfolioAllocationUseCase,
                                 GetPortfolioHistoryUseCase getPortfolioHistoryUseCase,
                                 GetDividendRadarUseCase getDividendRadarUseCase,
                                 GetAssetDetailsUseCase getAssetDetailsUseCase,
                                 SyncRealPortfolioUseCase syncRealPortfolioUseCase,
                                 Validator validator) {
        this.configureInvestmentsUseCase = configureInvestmentsUseCase;
        this.externalInvestmentApiPort = externalInvestmentApiPort;
        this.getPortfolioHoldingsUseCase = getPortfolioHoldingsUseCase;
        this.getPortfolioSummaryUseCase = getPortfolioSummaryUseCase;
        this.getPortfolioAllocationUseCase = getPortfolioAllocationUseCase;
        this.getPortfolioHistoryUseCase = getPortfolioHistoryUseCase;
        this.getDividendRadarUseCase = getDividendRadarUseCase;
        this.getAssetDetailsUseCase = getAssetDetailsUseCase;
        this.syncRealPortfolioUseCase = syncRealPortfolioUseCase;
        this.validator = validator;
    }

    /**
     * Replaces the caller's whole portfolio. {@code confirmReplace} is a query
     * parameter rather than a body field on purpose: the body is a bare JSON
     * array, so adding a field would mean changing its shape and breaking every
     * client at once. Defaulting to {@code false} is what makes the guard
     * protect app versions already installed on devices, which cannot send it.
     */
    @PostMapping("/configure")
    public ResponseEntity<Void> configureInvestments(
            @RequestBody List<AssetRegistrationDto> request,
            @RequestParam(name = "confirmReplace", defaultValue = "false") boolean confirmReplace) {
        String email = SecurityUtils.getCurrentUserEmail();
        validateAssets(request);
        // IllegalArgumentException propagates to GlobalExceptionHandler, which already maps it
        // to 400 with the use case's real message — no need to catch and discard it here.
        configureInvestmentsUseCase.execute(email, toCommands(request), confirmReplace);
        return ResponseEntity.ok().build();
    }

    // The request body is a raw List<AssetRegistrationDto>, not a wrapper object — Bean
    // Validation's @Valid does not reliably cascade into elements of a top-level List
    // request body, so each element is validated explicitly here instead of trusting an
    // annotation that may silently no-op. The financial rules themselves still live as
    // constraint annotations on AssetRegistrationDto (single source of truth); this just
    // guarantees they're actually enforced.
    private void validateAssets(List<AssetRegistrationDto> request) {
        if (request == null || request.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one investment is required");
        }
        Set<ConstraintViolation<AssetRegistrationDto>> violations = new LinkedHashSet<>();
        for (AssetRegistrationDto dto : request) {
            violations.addAll(validator.validate(dto));
        }
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private List<ConfigureInvestmentCommand> toCommands(List<AssetRegistrationDto> request) {
        return request.stream()
                .map(dto -> new ConfigureInvestmentCommand(
                        dto.name(), dto.quantity(), dto.purchasePrice(), dto.purchaseDate(), dto.type()))
                .toList();
    }

    @GetMapping("/quote/{ticker}")
    public ResponseEntity<AssetQuoteResponse> getQuote(@PathVariable String ticker) {
        Optional<AssetQuoteResponse> quoteOpt = externalInvestmentApiPort.getQuote(ticker);
        return quoteOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<AssetQuoteResponse>> searchQuotes(@RequestParam String query) {
        return ResponseEntity.ok(externalInvestmentApiPort.searchQuotes(query));
    }

    @GetMapping("/quote/{ticker}/at-date")
    public ResponseEntity<AssetQuoteResponse> getQuoteAtDate(
            @PathVariable String ticker,
            @RequestParam java.time.LocalDate date) {
        Optional<AssetQuoteResponse> quoteOpt = externalInvestmentApiPort.getQuoteAtDate(ticker, date);
        return quoteOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<InvestmentLotDTO>> getHoldings() {
        String email = SecurityUtils.getCurrentUserEmail();
        return ResponseEntity.ok(getPortfolioHoldingsUseCase.execute(email));
    }

    @GetMapping("/summary")
    public ResponseEntity<PortfolioSummaryDTO> getSummary() {
        String email = SecurityUtils.getCurrentUserEmail();
        return ResponseEntity.ok(getPortfolioSummaryUseCase.execute(email));
    }

    @GetMapping("/allocation")
    public ResponseEntity<List<AllocationSliceDTO>> getAllocation() {
        String email = SecurityUtils.getCurrentUserEmail();
        return ResponseEntity.ok(getPortfolioAllocationUseCase.execute(email));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PortfolioHistoryPointDTO>> getHistory(
            @RequestParam(required = false, defaultValue = "ALL") String range) {
        String email = SecurityUtils.getCurrentUserEmail();
        return ResponseEntity.ok(getPortfolioHistoryUseCase.execute(email, range));
    }

    @GetMapping("/dividends")
    public ResponseEntity<DividendRadarResponseDTO> getDividends() {
        String email = SecurityUtils.getCurrentUserEmail();
        return ResponseEntity.ok(getDividendRadarUseCase.execute(email));
    }

    @GetMapping("/asset-details/{ticker}")
    public ResponseEntity<AssetDetailsResponseDTO> getAssetDetails(@PathVariable String ticker) {
        String email = SecurityUtils.getCurrentUserEmail();
        return ResponseEntity.ok(getAssetDetailsUseCase.execute(email, ticker));
    }

    // Always 200 — DISABLED is the expected, normal outcome in every environment today (no
    // legitimate B3/brokerage integration exists yet), never surfaced as a client error.
    @PostMapping("/sync")
    public ResponseEntity<RealPortfolioSyncResultDTO> syncRealPortfolio(
            @RequestBody(required = false) SyncRealPortfolioRequest request) {
        String email = SecurityUtils.getCurrentUserEmail();
        String externalAccountReference = request != null ? request.externalAccountReference() : null;
        String idempotencyKey = request != null ? request.idempotencyKey() : null;
        return ResponseEntity.ok(syncRealPortfolioUseCase.execute(email, externalAccountReference, idempotencyKey));
    }
}
