package com.jf.PetApp.infrastructure.controller.investment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.dto.PortfolioSummaryDTO;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;
import com.jf.PetApp.application.investment.usecase.ConfigureInvestmentsUseCase;
import com.jf.PetApp.application.investment.usecase.GetAssetDetailsUseCase;
import com.jf.PetApp.application.investment.usecase.GetDividendRadarUseCase;
import com.jf.PetApp.application.investment.usecase.GetPortfolioAllocationUseCase;
import com.jf.PetApp.application.investment.usecase.GetPortfolioHistoryUseCase;
import com.jf.PetApp.application.investment.usecase.GetPortfolioHoldingsUseCase;
import com.jf.PetApp.application.investment.usecase.GetPortfolioSummaryUseCase;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

@WebMvcTest(controllers = InvestmentController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to test only web layer
class InvestmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConfigureInvestmentsUseCase configureInvestmentsUseCase;

    @MockitoBean
    private ExternalInvestmentApiPort externalInvestmentApiPort;

    @MockitoBean
    private GetPortfolioHoldingsUseCase getPortfolioHoldingsUseCase;

    @MockitoBean
    private GetPortfolioSummaryUseCase getPortfolioSummaryUseCase;

    @MockitoBean
    private GetPortfolioAllocationUseCase getPortfolioAllocationUseCase;

    @MockitoBean
    private GetPortfolioHistoryUseCase getPortfolioHistoryUseCase;

    @MockitoBean
    private GetDividendRadarUseCase getDividendRadarUseCase;

    @MockitoBean
    private GetAssetDetailsUseCase getAssetDetailsUseCase;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock the exact filter that security config uses

    @Test
    @WithMockUser(username = "investor@test.com")
    void configureInvestments_WithValidAssets_Returns200() throws Exception {
        mockMvc.perform(post("/api/investments/configure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"name":"PETR4","quantity":100,"purchasePrice":30.5,"purchaseDate":"2025-01-01","type":"STOCKS"}]"""))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void configureInvestments_WithEmptyList_Returns400() throws Exception {
        mockMvc.perform(post("/api/investments/configure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void configureInvestments_WithInvalidAssetFields_Returns400ValidationError() throws Exception {
        mockMvc.perform(post("/api/investments/configure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"name":"","quantity":-1,"purchasePrice":30.5,"purchaseDate":"2025-01-01","type":"STOCKS"}]"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void configureInvestments_WhenUseCaseRejects_PropagatesRealMessageAsBadRequest() throws Exception {
        // Regression test for Phase A.6: InvestmentController must let IllegalArgumentException
        // propagate to GlobalExceptionHandler rather than discarding the real message.
        org.mockito.Mockito.doThrow(new IllegalArgumentException("User not found for email: investor@test.com"))
                .when(configureInvestmentsUseCase).execute(eq("investor@test.com"), any());

        mockMvc.perform(post("/api/investments/configure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"name":"PETR4","quantity":100,"purchasePrice":30.5,"purchaseDate":"2025-01-01","type":"STOCKS"}]"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("User not found for email: investor@test.com"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void getQuote_WhenAvailable_ReturnsQuote() throws Exception {
        when(externalInvestmentApiPort.getQuote("PETR4"))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 35.0, "BRL")));

        mockMvc.perform(get("/api/investments/quote/PETR4").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("PETR4"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void getQuote_WhenUnavailable_Returns404() throws Exception {
        when(externalInvestmentApiPort.getQuote("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/investments/quote/UNKNOWN").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void searchQuotes_ReturnsMatches() throws Exception {
        when(externalInvestmentApiPort.searchQuotes("PETR"))
                .thenReturn(List.of(new AssetQuoteResponse("PETR4", "Petrobras", 35.0, "BRL")));

        mockMvc.perform(get("/api/investments/search").param("query", "PETR").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("PETR4"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void getQuoteAtDate_WhenAvailable_ReturnsQuote() throws Exception {
        when(externalInvestmentApiPort.getQuoteAtDate("PETR4", java.time.LocalDate.of(2025, 1, 3)))
                .thenReturn(Optional.of(new AssetQuoteResponse("PETR4", "Petrobras", 31.0, "BRL")));

        mockMvc.perform(get("/api/investments/quote/PETR4/at-date")
                        .param("date", "2025-01-03")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regularMarketPrice").value(31.0));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void getQuoteAtDate_WhenUnavailable_Returns404() throws Exception {
        when(externalInvestmentApiPort.getQuoteAtDate("UNKNOWN", java.time.LocalDate.of(2025, 1, 3)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/investments/quote/UNKNOWN/at-date")
                        .param("date", "2025-01-03")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void getSummary_ReturnsPortfolioSummary() throws Exception {
        when(getPortfolioSummaryUseCase.execute(eq("investor@test.com")))
                .thenReturn(new PortfolioSummaryDTO(1000.0, 1200.0, 200.0, 20.0, 3));

        mockMvc.perform(get("/api/investments/summary").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue").value(1200.0))
                .andExpect(jsonPath("$.totalGainPercent").value(20.0));
    }
}
