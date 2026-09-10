package com.jf.PetApp.infrastructure.controller.investment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.investment.dto.AssetQuoteResponse;
import com.jf.PetApp.application.investment.dto.InvestmentDTO;
import com.jf.PetApp.application.investment.exception.DestructivePortfolioReplaceException;
import com.jf.PetApp.application.investment.dto.PortfolioSummaryDTO;
import com.jf.PetApp.application.investment.port.ExternalInvestmentApiPort;
import com.jf.PetApp.application.investment.usecase.ConfigureInvestmentsUseCase;
import com.jf.PetApp.application.investment.usecase.CreateInvestmentLotUseCase;
import com.jf.PetApp.application.investment.usecase.GetAssetDetailsUseCase;
import com.jf.PetApp.application.investment.usecase.UpdateInvestmentLotUseCase;
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
    private CreateInvestmentLotUseCase createInvestmentLotUseCase;

    @MockitoBean
    private UpdateInvestmentLotUseCase updateInvestmentLotUseCase;

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
    private com.jf.PetApp.application.investment.usecase.SyncRealPortfolioUseCase syncRealPortfolioUseCase;

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
                .when(configureInvestmentsUseCase).execute(eq("investor@test.com"), any(), anyBoolean());

        mockMvc.perform(post("/api/investments/configure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"name":"PETR4","quantity":100,"purchasePrice":30.5,"purchaseDate":"2025-01-01","type":"STOCKS"}]"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("User not found for email: investor@test.com"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void configureInvestments_ByDefault_DoesNotConfirmReplacement() throws Exception {
        // The flag defaults to false precisely so app versions already installed
        // — which cannot send it — stay protected by the guard.
        mockMvc.perform(post("/api/investments/configure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"name":"PETR4","quantity":100,"purchasePrice":30.5,"purchaseDate":"2025-01-01","type":"STOCKS"}]"""))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(configureInvestmentsUseCase)
                .execute(eq("investor@test.com"), any(), eq(false));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void configureInvestments_WhenConfirmReplaceIsTrue_PassesItThrough() throws Exception {
        mockMvc.perform(post("/api/investments/configure?confirmReplace=true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"name":"PETR4","quantity":100,"purchasePrice":30.5,"purchaseDate":"2025-01-01","type":"STOCKS"}]"""))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(configureInvestmentsUseCase)
                .execute(eq("investor@test.com"), any(), eq(true));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void configureInvestments_WhenReplacementIsUnconfirmed_ReturnsConflictWithStableCode() throws Exception {
        org.mockito.Mockito.doThrow(new DestructivePortfolioReplaceException(12, 1))
                .when(configureInvestmentsUseCase).execute(eq("investor@test.com"), any(), anyBoolean());

        mockMvc.perform(post("/api/investments/configure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"name":"PETR4","quantity":100,"purchasePrice":30.5,"purchaseDate":"2025-01-01","type":"STOCKS"}]"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PORTFOLIO_REPLACE_NOT_CONFIRMED"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void createInvestment_WithValidAsset_Returns201WithCreatedLot() throws Exception {
        when(createInvestmentLotUseCase.execute(eq("investor@test.com"), any())).thenReturn(
                new InvestmentDTO(7, "PETR4", BigDecimal.valueOf(100), BigDecimal.valueOf(30.5),
                        java.time.LocalDate.of(2025, 1, 1), com.jf.PetApp.core.domain.enums.InvestmentType.STOCKS));

        mockMvc.perform(post("/api/investments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"PETR4","quantity":100,"purchasePrice":30.5,"purchaseDate":"2025-01-01","type":"STOCKS"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("PETR4"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void createInvestment_WithInvalidFields_Returns400ValidationError() throws Exception {
        mockMvc.perform(post("/api/investments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","quantity":-1,"purchasePrice":30.5,"purchaseDate":"2025-01-01","type":"STOCKS"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        org.mockito.Mockito.verifyNoInteractions(createInvestmentLotUseCase);
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void createInvestment_NeverCallsConfigureInvestmentsUseCase() throws Exception {
        // Regression guard: the granular endpoint must never fall back to the full-replace path.
        when(createInvestmentLotUseCase.execute(eq("investor@test.com"), any())).thenReturn(
                new InvestmentDTO(1, "PETR4", BigDecimal.ONE, BigDecimal.TEN,
                        java.time.LocalDate.now(), com.jf.PetApp.core.domain.enums.InvestmentType.STOCKS));

        mockMvc.perform(post("/api/investments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"PETR4","quantity":1,"purchasePrice":10,"purchaseDate":"2025-01-01","type":"STOCKS"}"""))
                .andExpect(status().isCreated());

        org.mockito.Mockito.verifyNoInteractions(configureInvestmentsUseCase);
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void updateInvestment_WithValidAsset_Returns200WithUpdatedLot() throws Exception {
        when(updateInvestmentLotUseCase.execute(eq("investor@test.com"), eq(7), any())).thenReturn(
                new InvestmentDTO(7, "PETR4", BigDecimal.valueOf(150), BigDecimal.valueOf(31.0),
                        java.time.LocalDate.of(2025, 2, 1), com.jf.PetApp.core.domain.enums.InvestmentType.STOCKS));

        mockMvc.perform(put("/api/investments/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"PETR4","quantity":150,"purchasePrice":31.0,"purchaseDate":"2025-02-01","type":"STOCKS"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.quantity").value(150));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void updateInvestment_WhenLotNotFoundOrNotOwned_Returns404() throws Exception {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Investment not found: 7"))
                .when(updateInvestmentLotUseCase).execute(eq("investor@test.com"), eq(7), any());

        mockMvc.perform(put("/api/investments/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"PETR4","quantity":150,"purchasePrice":31.0,"purchaseDate":"2025-02-01","type":"STOCKS"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void updateInvestment_WithInvalidFields_Returns400ValidationError() throws Exception {
        mockMvc.perform(put("/api/investments/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","quantity":-1,"purchasePrice":31.0,"purchaseDate":"2025-02-01","type":"STOCKS"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        org.mockito.Mockito.verifyNoInteractions(updateInvestmentLotUseCase);
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
                .thenReturn(new PortfolioSummaryDTO(
                        BigDecimal.valueOf(1000.0), BigDecimal.valueOf(1200.0), BigDecimal.valueOf(200.0), BigDecimal.valueOf(20.0), 3));

        mockMvc.perform(get("/api/investments/summary").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue").value(1200.0))
                .andExpect(jsonPath("$.totalGainPercent").value(20.0));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void syncRealPortfolio_WhenDisabled_Returns200WithDisabledStatus() throws Exception {
        when(syncRealPortfolioUseCase.execute(eq("investor@test.com"), any(), any())).thenReturn(
                new com.jf.PetApp.application.investment.dto.RealPortfolioSyncResultDTO(
                        "DISABLED", "B3", "No legitimate B3 integration is configured yet.",
                        java.time.Instant.now(), java.time.Instant.now()));

        mockMvc.perform(post("/api/investments/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.provider").value("B3"));
    }

    @Test
    @WithMockUser(username = "investor@test.com")
    void syncRealPortfolio_WithNoRequestBody_StillWorks() throws Exception {
        when(syncRealPortfolioUseCase.execute(eq("investor@test.com"), any(), any())).thenReturn(
                new com.jf.PetApp.application.investment.dto.RealPortfolioSyncResultDTO(
                        "DISABLED", "B3", "No legitimate B3 integration is configured yet.",
                        java.time.Instant.now(), java.time.Instant.now()));

        mockMvc.perform(post("/api/investments/sync").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
