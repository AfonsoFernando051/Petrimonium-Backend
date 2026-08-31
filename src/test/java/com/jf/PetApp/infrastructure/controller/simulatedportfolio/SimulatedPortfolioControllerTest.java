package com.jf.PetApp.infrastructure.controller.simulatedportfolio;

import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;
import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedOrderDTO;
import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedPortfolioSummaryDTO;
import com.jf.PetApp.application.simulatedportfolio.usecase.GetSimulatedOrderHistoryUseCase;
import com.jf.PetApp.application.simulatedportfolio.usecase.GetSimulatedPortfolioUseCase;
import com.jf.PetApp.application.simulatedportfolio.usecase.PlaceSimulatedOrderUseCase;
import com.jf.PetApp.application.simulatedportfolio.usecase.ResetSimulatedPortfolioUseCase;
import com.jf.PetApp.core.domain.enums.SimulatedOrderSide;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SimulatedPortfolioController.class)
@AutoConfigureMockMvc(addFilters = false) // Web-layer only — SecurityConfigTest covers the real authorization gate.
class SimulatedPortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSimulatedPortfolioUseCase getSimulatedPortfolioUseCase;

    @MockitoBean
    private PlaceSimulatedOrderUseCase placeSimulatedOrderUseCase;

    @MockitoBean
    private GetSimulatedOrderHistoryUseCase getSimulatedOrderHistoryUseCase;

    @MockitoBean
    private ResetSimulatedPortfolioUseCase resetSimulatedPortfolioUseCase;

    @MockitoBean
    private ExternalInvestmentApiPort externalInvestmentApiPort;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(username = "learner@test.com")
    void getMyPortfolio_ReturnsTheSummary() throws Exception {
        when(getSimulatedPortfolioUseCase.execute("learner@test.com")).thenReturn(
                new SimulatedPortfolioSummaryDTO(
                        new BigDecimal("10000.00"), new BigDecimal("10000.00"), "BRL", null, List.of()));

        mockMvc.perform(get("/api/v1/simulated-portfolios/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.virtualBalance").value(10000.00))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void placeOrder_WithValidRequest_ReturnsCreated() throws Exception {
        when(placeSimulatedOrderUseCase.execute(eq("learner@test.com"), any())).thenReturn(
                new SimulatedOrderDTO(1L, "PETR4", SimulatedOrderSide.BUY,
                        new BigDecimal("10"), new BigDecimal("30.50"), new BigDecimal("305.00"),
                        Instant.now(), "generated-id"));

        mockMvc.perform(post("/api/v1/simulated-portfolios/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"PETR4\",\"side\":\"BUY\",\"quantity\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("PETR4"))
                .andExpect(jsonPath("$.total").value(305.00));
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void placeOrder_WithZeroQuantity_IsRejectedByValidationBeforeReachingTheUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/simulated-portfolios/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"PETR4\",\"side\":\"BUY\",\"quantity\":0}"))
                .andExpect(status().isBadRequest());

        verify(placeSimulatedOrderUseCase, never()).execute(any(), any());
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void placeOrder_WithBlankTicker_IsRejectedByValidation() throws Exception {
        mockMvc.perform(post("/api/v1/simulated-portfolios/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"\",\"side\":\"BUY\",\"quantity\":10}"))
                .andExpect(status().isBadRequest());

        verify(placeSimulatedOrderUseCase, never()).execute(any(), any());
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void getOrders_ReturnsTheHistory() throws Exception {
        when(getSimulatedOrderHistoryUseCase.execute("learner@test.com")).thenReturn(List.of(
                new SimulatedOrderDTO(1L, "PETR4", SimulatedOrderSide.BUY,
                        new BigDecimal("10"), new BigDecimal("30.50"), new BigDecimal("305.00"),
                        Instant.now(), "order-1")
        ));

        mockMvc.perform(get("/api/v1/simulated-portfolios/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("PETR4"));
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void reset_WithConfirmTrue_ReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/simulated-portfolios/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirm\":true}"))
                .andExpect(status().isNoContent());

        verify(resetSimulatedPortfolioUseCase).execute("learner@test.com", true);
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void reset_WithConfirmFalse_IsRejectedByValidationBeforeReachingTheUseCase() throws Exception {
        mockMvc.perform(post("/api/v1/simulated-portfolios/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirm\":false}"))
                .andExpect(status().isBadRequest());

        verify(resetSimulatedPortfolioUseCase, never()).execute(any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void searchQuotes_ReturnsWhateverTheMarketDataPortReturns() throws Exception {
        when(externalInvestmentApiPort.searchQuotes("petr")).thenReturn(
                List.of(new AssetQuoteResponse("PETR4", "Petrobras", 30.50, "BRL")));

        mockMvc.perform(get("/api/v1/simulated-portfolios/quotes/search").param("query", "petr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("PETR4"));
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void getQuote_WhenFound_ReturnsIt() throws Exception {
        when(externalInvestmentApiPort.getQuote("PETR4")).thenReturn(
                Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 30.50, "BRL")));

        mockMvc.perform(get("/api/v1/simulated-portfolios/quotes/PETR4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regularMarketPrice").value(30.50));
    }

    @Test
    @WithMockUser(username = "learner@test.com")
    void getQuote_WhenUnknown_ReturnsNotFound() throws Exception {
        when(externalInvestmentApiPort.getQuote("GHOST99")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/simulated-portfolios/quotes/GHOST99"))
                .andExpect(status().isNotFound());
    }
}
