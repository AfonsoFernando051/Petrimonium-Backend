package com.jf.PetApp.infrastructure.controller.health;

import static com.jf.PetApp.core.domain.health.HealthModels.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jf.PetApp.application.health.HealthService;
import com.jf.PetApp.application.health.exception.HealthConflictException;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * The wire format, which the Flutter client depends on literally: money is a decimal
 * <em>string</em> ({@code "1480.50"}, never {@code 1480.5}), a date is {@code YYYY-MM-DD} and a
 * month {@code YYYY-MM}. A JSON number here would be parsed into a binary double on the device
 * and could show the user a cent that does not exist.
 *
 * <p>Web layer only — {@code HealthSecurityBoundaryTest} covers the real app_context gate and
 * {@code HealthServiceIntegrationTest} the rules behind these responses.
 */
@WebMvcTest(controllers = HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

    private static final String USER = "ana@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static Profile profile() {
        return new Profile(7L, CountryCode.PT, CurrencyCode.EUR, "pt-PT", Instant.EPOCH, Instant.EPOCH);
    }

    private static Account account() {
        return new Account(10L, 7L, "Conta à ordem", AccountType.CHECKING, new BigDecimal("850.00"),
                LocalDate.of(2026, 9, 1), CurrencyCode.EUR, false, "key", Instant.EPOCH, Instant.EPOCH);
    }

    private static Transaction transaction(EntryType type, EntryStatus statusValue) {
        return new Transaction(31L, 7L, 10L, type, statusValue, new BigDecimal("42.90"), CurrencyCode.EUR,
                "Eletricidade", "utilities", LocalDate.of(2026, 9, 12), RecordSource.MANUAL, null, null,
                "key", null, null, null, null, null, Instant.EPOCH, Instant.EPOCH);
    }

    @Test
    @WithMockUser(username = USER)
    void getProfile_ReturnsTheStoredPreferencesAndWhetherTheCurrencyIsStillChangeable() throws Exception {
        when(healthService.getProfile(USER)).thenReturn(Optional.of(new HealthService.ProfileView(profile(), false)));

        mockMvc.perform(get("/api/v1/health/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("PT"))
                .andExpect(jsonPath("$.primaryCurrency").value("EUR"))
                .andExpect(jsonPath("$.localeTag").value("pt-PT"))
                .andExpect(jsonPath("$.currencyChangeAllowed").value(false));
    }

    @Test
    @WithMockUser(username = USER)
    void getProfile_BeforeOnboarding_Is404SoTheAppShowsOnboardingInsteadOfADefaultCountry() throws Exception {
        when(healthService.getProfile(USER)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/health/profile")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = USER)
    void putProfile_PassesCountryCurrencyAndLocaleThroughAsThreeIndependentFields() throws Exception {
        when(healthService.saveProfile(eq(USER), any()))
                .thenReturn(new HealthService.ProfileView(profile(), true));

        mockMvc.perform(put("/api/v1/health/profile").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"countryCode\":\"PT\",\"primaryCurrency\":\"EUR\",\"localeTag\":\"pt-PT\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<HealthService.ProfileInput> captor =
                ArgumentCaptor.forClass(HealthService.ProfileInput.class);
        verify(healthService).saveProfile(eq(USER), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("PT", captor.getValue().countryCode());
        org.junit.jupiter.api.Assertions.assertEquals("EUR", captor.getValue().primaryCurrency());
        org.junit.jupiter.api.Assertions.assertEquals("pt-PT", captor.getValue().localeTag());
    }

    @Test
    @WithMockUser(username = USER)
    void listAccounts_SerializesMoneyAsAStringAndDatesAsCivilDates() throws Exception {
        when(healthService.listAccounts(USER)).thenReturn(List.of(
                new HealthService.AccountView(account(), new BigDecimal("1480.50"))));

        mockMvc.perform(get("/api/v1/health/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].currentBalance").isString())
                .andExpect(jsonPath("$[0].currentBalance").value("1480.50"))
                .andExpect(jsonPath("$[0].initialBalance").value("850.00"))
                .andExpect(jsonPath("$[0].balanceReferenceDate").value("2026-09-01"))
                .andExpect(jsonPath("$[0].currency").value("EUR"))
                .andExpect(jsonPath("$[0].archived").value(false));
    }

    @Test
    @WithMockUser(username = USER)
    void listAccounts_KeepsTrailingCentsRatherThanTrimmingThem() throws Exception {
        // 1480.5 and 1480.50 are the same number but not the same money string; the client
        // parses two decimal places, so a trimmed zero would be a contract break.
        when(healthService.listAccounts(USER)).thenReturn(List.of(
                new HealthService.AccountView(account(), new BigDecimal("1480.50"))));

        mockMvc.perform(get("/api/v1/health/accounts"))
                .andExpect(jsonPath("$[0].currentBalance").value("1480.50"));
    }

    @Test
    @WithMockUser(username = USER)
    void createAccount_Returns201() throws Exception {
        when(healthService.createAccount(eq(USER), any()))
                .thenReturn(new HealthService.AccountView(account(), new BigDecimal("850.00")));

        mockMvc.perform(post("/api/v1/health/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Conta à ordem\",\"type\":\"CHECKING\",\"initialBalance\":\"850.00\","
                                + "\"balanceReferenceDate\":\"2026-09-01\",\"currency\":\"EUR\","
                                + "\"idempotencyKey\":\"abc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentBalance").value("850.00"));
    }

    @Test
    @WithMockUser(username = USER)
    void deleteAccount_Archives_AndAnswers204() throws Exception {
        mockMvc.perform(delete("/api/v1/health/accounts/10")).andExpect(status().isNoContent());

        verify(healthService).archiveAccount(USER, 10L);
    }

    @Test
    @WithMockUser(username = USER)
    void listTransactions_ForwardsEveryFilterItWasGiven() throws Exception {
        when(healthService.listTransactions(eq(USER), any())).thenReturn(List.of(
                transaction(EntryType.EXPENSE, EntryStatus.PLANNED)));

        mockMvc.perform(get("/api/v1/health/transactions")
                        .param("from", "2026-09-01").param("to", "2026-09-30")
                        .param("accountId", "10").param("category", "utilities").param("status", "PLANNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value("42.90"))
                .andExpect(jsonPath("$[0].date").value("2026-09-12"))
                .andExpect(jsonPath("$[0].status").value("PLANNED"))
                .andExpect(jsonPath("$[0].source").value("MANUAL"));

        ArgumentCaptor<HealthService.TransactionFilter> captor =
                ArgumentCaptor.forClass(HealthService.TransactionFilter.class);
        verify(healthService).listTransactions(eq(USER), captor.capture());
        HealthService.TransactionFilter filter = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2026, 9, 1), filter.from());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2026, 9, 30), filter.to());
        org.junit.jupiter.api.Assertions.assertEquals(10L, filter.accountId());
        org.junit.jupiter.api.Assertions.assertEquals("utilities", filter.category());
        org.junit.jupiter.api.Assertions.assertEquals("PLANNED", filter.status());
    }

    @Test
    @WithMockUser(username = USER)
    void confirmTransaction_ReturnsTheSameEntryNowRealized() throws Exception {
        when(healthService.confirmTransaction(USER, 31L))
                .thenReturn(transaction(EntryType.EXPENSE, EntryStatus.REALIZED));

        mockMvc.perform(post("/api/v1/health/transactions/31/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.status").value("REALIZED"));
    }

    @Test
    @WithMockUser(username = USER)
    void createTransfer_ReturnsOneTransferWithItsTwoLegs() throws Exception {
        Transfer transfer = new Transfer(5L, 7L, 10L, 11L, new BigDecimal("100.00"), CurrencyCode.EUR,
                LocalDate.of(2026, 9, 3), "Poupança", "key", Instant.EPOCH);
        when(healthService.createTransfer(eq(USER), any())).thenReturn(new HealthService.TransferView(transfer,
                transaction(EntryType.TRANSFER_OUT, EntryStatus.REALIZED),
                transaction(EntryType.TRANSFER_IN, EntryStatus.REALIZED)));

        mockMvc.perform(post("/api/v1/health/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":10,\"toAccountId\":11,\"amount\":\"100.00\","
                                + "\"currency\":\"EUR\",\"date\":\"2026-09-03\",\"idempotencyKey\":\"t1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value("100.00"))
                .andExpect(jsonPath("$.outLeg.type").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$.inLeg.type").value("TRANSFER_IN"));
    }

    @Test
    @WithMockUser(username = USER)
    void createPurchase_ReturnsEveryInstallmentAndTheInvoiceItLandsOn() throws Exception {
        Purchase purchase = new Purchase(80L, 7L, 20L, new BigDecimal("999.99"), CurrencyCode.EUR,
                "Computador", "shopping", LocalDate.of(2026, 9, 3), 3, RecordSource.MANUAL, null, null,
                "key", Instant.EPOCH);
        when(healthService.createPurchase(eq(USER), eq(20L), any())).thenReturn(new PurchaseWithInstallments(
                purchase, List.of(
                        new Installment(1L, 7L, 80L, 90L, CurrencyCode.EUR, 1, 3, new BigDecimal("333.33")),
                        new Installment(2L, 7L, 80L, 91L, CurrencyCode.EUR, 2, 3, new BigDecimal("333.33")),
                        new Installment(3L, 7L, 80L, 92L, CurrencyCode.EUR, 3, 3, new BigDecimal("333.33")))));

        mockMvc.perform(post("/api/v1/health/cards/20/purchases").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"999.99\",\"currency\":\"EUR\",\"description\":\"Computador\","
                                + "\"category\":\"shopping\",\"purchaseDate\":\"2026-09-03\","
                                + "\"installmentCount\":3,\"idempotencyKey\":\"p1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value("999.99"))
                .andExpect(jsonPath("$.installments.length()").value(3))
                .andExpect(jsonPath("$.installments[0].amount").value("333.33"))
                .andExpect(jsonPath("$.installments[0].invoiceId").value(90));
    }

    @Test
    @WithMockUser(username = USER)
    void listInvoices_ExposesTheCycleTheDueDateAndWhetherItIsPaid() throws Exception {
        Invoice invoice = new Invoice(90L, 7L, 20L, CurrencyCode.EUR, YearMonth.of(2026, 9),
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 5), InvoiceStatus.OPEN, null, null,
                Instant.EPOCH, Instant.EPOCH);
        when(healthService.listInvoices(USER, 20L)).thenReturn(List.of(
                new InvoiceWithTotal(invoice, new BigDecimal("333.33"))));

        mockMvc.perform(get("/api/v1/health/cards/20/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cycleMonth").value("2026-09"))
                .andExpect(jsonPath("$[0].dueDate").value("2026-10-05"))
                .andExpect(jsonPath("$[0].amount").value("333.33"))
                .andExpect(jsonPath("$[0].paid").value(false))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    @WithMockUser(username = USER)
    void payInvoice_MarksItPaidAndReportsTheDate() throws Exception {
        Invoice paid = new Invoice(90L, 7L, 20L, CurrencyCode.EUR, YearMonth.of(2026, 9),
                LocalDate.of(2026, 9, 25), LocalDate.of(2026, 10, 5), InvoiceStatus.PAID,
                LocalDate.of(2026, 10, 5), 99L, Instant.EPOCH, Instant.EPOCH);
        when(healthService.payInvoice(eq(USER), eq(90L), any()))
                .thenReturn(new InvoiceWithTotal(paid, new BigDecimal("333.33")));

        mockMvc.perform(post("/api/v1/health/cards/invoices/90/pay").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":10,\"currency\":\"EUR\",\"paymentDate\":\"2026-10-05\","
                                + "\"idempotencyKey\":\"pay-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.paidAt").value("2026-10-05"));
    }

    @Test
    @WithMockUser(username = USER)
    void summary_KeepsCurrentResultAndProjectedBalanceAsSeparateFields() throws Exception {
        when(healthService.summary(USER, YearMonth.of(2026, 9))).thenReturn(new MonthlySummary(
                YearMonth.of(2026, 9), CurrencyCode.EUR, new BigDecimal("2307.10"), new BigDecimal("1500.00"),
                new BigDecimal("376.23"), new BigDecimal("0.00"), new BigDecimal("200.00"),
                new BigDecimal("333.33"), new BigDecimal("1123.77"), new BigDecimal("1773.77"),
                List.of(new CategoryAmount("utilities", new BigDecimal("42.90"))),
                List.of(new Upcoming("CARD_INVOICE", 90L, "Fatura do cartão", LocalDate.of(2026, 10, 5),
                        new BigDecimal("333.33")))));

        mockMvc.perform(get("/api/v1/health/summary").param("month", "2026-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-09"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.currentBalance").value("2307.10"))
                .andExpect(jsonPath("$.monthResult").value("1123.77"))
                .andExpect(jsonPath("$.projectedEndBalance").value("1773.77"))
                .andExpect(jsonPath("$.openCardInvoices").value("333.33"))
                .andExpect(jsonPath("$.expensesByCategory[0].category").value("utilities"))
                .andExpect(jsonPath("$.expensesByCategory[0].amount").value("42.90"))
                .andExpect(jsonPath("$.upcoming[0].kind").value("CARD_INVOICE"))
                .andExpect(jsonPath("$.upcoming[0].date").value("2026-10-05"));
    }

    @Test
    @WithMockUser(username = USER)
    void summary_WithoutAMonth_LetsTheServiceDecideTheCurrentOne() throws Exception {
        when(healthService.summary(USER, null)).thenReturn(new MonthlySummary(YearMonth.of(2026, 9),
                CurrencyCode.BRL, BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2),
                BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2),
                List.of(), List.of()));

        mockMvc.perform(get("/api/v1/health/summary")).andExpect(status().isOk());

        verify(healthService).summary(USER, null);
    }

    @Test
    @WithMockUser(username = USER)
    void summary_WithAMalformedMonth_Is400AndNeverReachesTheService() throws Exception {
        mockMvc.perform(get("/api/v1/health/summary").param("month", "setembro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verify(healthService, never()).summary(any(), any());
    }

    @Test
    @WithMockUser(username = USER)
    void aCurrencyMismatch_IsA409CarryingItsStableCode() throws Exception {
        // The app localizes from `code`; it must never have to match on an English message.
        when(healthService.createTransaction(eq(USER), any())).thenThrow(
                new HealthConflictException("CURRENCY_MISMATCH", "A moeda do registro não corresponde."));

        mockMvc.perform(post("/api/v1/health/transactions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":10,\"type\":\"EXPENSE\",\"status\":\"REALIZED\","
                                + "\"amount\":\"10.00\",\"currency\":\"BRL\",\"description\":\"x\","
                                + "\"date\":\"2026-09-12\",\"idempotencyKey\":\"k\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CURRENCY_MISMATCH"));
    }

    @Test
    @WithMockUser(username = USER)
    void aLockedCurrency_IsA409TheAppCanExplainToTheUser() throws Exception {
        when(healthService.saveProfile(eq(USER), any())).thenThrow(
                new HealthConflictException("CURRENCY_CHANGE_LOCKED", "Já existem dados financeiros."));

        mockMvc.perform(put("/api/v1/health/profile").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"countryCode\":\"BR\",\"primaryCurrency\":\"BRL\",\"localeTag\":\"pt-BR\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CURRENCY_CHANGE_LOCKED"));
    }
}
