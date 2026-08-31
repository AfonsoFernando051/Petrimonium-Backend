package com.jf.PetApp.infrastructure.controller.simulatedportfolio;

import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;
import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedOrderDTO;
import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedPortfolioSummaryDTO;
import com.jf.PetApp.application.simulatedportfolio.usecase.GetSimulatedOrderHistoryUseCase;
import com.jf.PetApp.application.simulatedportfolio.usecase.GetSimulatedPortfolioUseCase;
import com.jf.PetApp.application.simulatedportfolio.usecase.PlaceSimulatedOrderCommand;
import com.jf.PetApp.application.simulatedportfolio.usecase.PlaceSimulatedOrderUseCase;
import com.jf.PetApp.application.simulatedportfolio.usecase.ResetSimulatedPortfolioUseCase;
import com.jf.PetApp.core.security.SecurityUtils;
import com.jf.PetApp.infrastructure.controller.simulatedportfolio.dto.PlaceSimulatedOrderRequest;
import com.jf.PetApp.infrastructure.controller.simulatedportfolio.dto.ResetSimulatedPortfolioRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Academy-only (see SecurityConfig: {@code hasAuthority(APP_CONTEXT_ACADEMY)}
 * on this whole path). Never returns or accepts real_portfolio data — every
 * response here is entirely simulated, and every screen consuming it must
 * make that explicit to the user (see docs/BACKEND_MODULE_PLAN.md and the
 * Academy repo's simulated-wallet UI work).
 */
@RestController
@RequestMapping("/api/v1/simulated-portfolios")
public class SimulatedPortfolioController {

    private final GetSimulatedPortfolioUseCase getSimulatedPortfolioUseCase;
    private final PlaceSimulatedOrderUseCase placeSimulatedOrderUseCase;
    private final GetSimulatedOrderHistoryUseCase getSimulatedOrderHistoryUseCase;
    private final ResetSimulatedPortfolioUseCase resetSimulatedPortfolioUseCase;
    private final ExternalInvestmentApiPort externalInvestmentApiPort;

    public SimulatedPortfolioController(
            GetSimulatedPortfolioUseCase getSimulatedPortfolioUseCase,
            PlaceSimulatedOrderUseCase placeSimulatedOrderUseCase,
            GetSimulatedOrderHistoryUseCase getSimulatedOrderHistoryUseCase,
            ResetSimulatedPortfolioUseCase resetSimulatedPortfolioUseCase,
            ExternalInvestmentApiPort externalInvestmentApiPort
    ) {
        this.getSimulatedPortfolioUseCase = getSimulatedPortfolioUseCase;
        this.placeSimulatedOrderUseCase = placeSimulatedOrderUseCase;
        this.getSimulatedOrderHistoryUseCase = getSimulatedOrderHistoryUseCase;
        this.resetSimulatedPortfolioUseCase = resetSimulatedPortfolioUseCase;
        this.externalInvestmentApiPort = externalInvestmentApiPort;
    }

    // Academy has no reachable equivalent of /api/investments/search or
    // /api/investments/quote/{ticker} — those are Wallet-only per SecurityConfig. These two
    // read-only passthroughs give Academy's order-placement UI a way to search tickers and
    // preview the reference price it's about to trade at, without granting it any access to
    // real_portfolio. Same public market-data exception as PlaceSimulatedOrderUseCaseImpl (see
    // its class doc and SimulatedPortfolioBoundaryTest) — mirrors InvestmentController's own
    // getQuote/searchQuotes, which also call this port directly with no dedicated use case.
    @GetMapping("/quotes/search")
    public ResponseEntity<List<AssetQuoteResponse>> searchQuotes(@RequestParam String query) {
        return ResponseEntity.ok(externalInvestmentApiPort.searchQuotes(query));
    }

    @GetMapping("/quotes/{ticker}")
    public ResponseEntity<AssetQuoteResponse> getQuote(@PathVariable String ticker) {
        Optional<AssetQuoteResponse> quote = externalInvestmentApiPort.getQuote(ticker);
        return quote.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    public ResponseEntity<SimulatedPortfolioSummaryDTO> getMyPortfolio() {
        String email = SecurityUtils.getCurrentUserEmail();
        return ResponseEntity.ok(getSimulatedPortfolioUseCase.execute(email));
    }

    @PostMapping("/orders")
    public ResponseEntity<SimulatedOrderDTO> placeOrder(@Valid @RequestBody PlaceSimulatedOrderRequest request) {
        String email = SecurityUtils.getCurrentUserEmail();
        SimulatedOrderDTO result = placeSimulatedOrderUseCase.execute(
                email,
                new PlaceSimulatedOrderCommand(request.ticker(), request.side(), request.quantity(), request.clientOrderId())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<SimulatedOrderDTO>> getOrders() {
        String email = SecurityUtils.getCurrentUserEmail();
        return ResponseEntity.ok(getSimulatedOrderHistoryUseCase.execute(email));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetSimulatedPortfolioRequest request) {
        String email = SecurityUtils.getCurrentUserEmail();
        resetSimulatedPortfolioUseCase.execute(email, request.confirm());
        return ResponseEntity.noContent().build();
    }
}
