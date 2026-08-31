package com.jf.PetApp.infrastructure.controller.simulatedportfolio;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    public SimulatedPortfolioController(
            GetSimulatedPortfolioUseCase getSimulatedPortfolioUseCase,
            PlaceSimulatedOrderUseCase placeSimulatedOrderUseCase,
            GetSimulatedOrderHistoryUseCase getSimulatedOrderHistoryUseCase,
            ResetSimulatedPortfolioUseCase resetSimulatedPortfolioUseCase
    ) {
        this.getSimulatedPortfolioUseCase = getSimulatedPortfolioUseCase;
        this.placeSimulatedOrderUseCase = placeSimulatedOrderUseCase;
        this.getSimulatedOrderHistoryUseCase = getSimulatedOrderHistoryUseCase;
        this.resetSimulatedPortfolioUseCase = resetSimulatedPortfolioUseCase;
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
