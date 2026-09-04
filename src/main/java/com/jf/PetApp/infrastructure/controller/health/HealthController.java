package com.jf.PetApp.infrastructure.controller.health;

import com.jf.PetApp.application.health.HealthService;
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

/**
 * Health-only (see SecurityConfig: {@code hasAuthority(APP_CONTEXT_HEALTH)} on this whole path).
 * This is the user's real money — salary, rent, card invoices — and is never reachable from an
 * Academy session (whose portfolio is simulated) or a Wallet one.
 *
 * <p>The owner is always {@link SecurityUtils#getCurrentUserEmail()}, resolved from the JWT
 * subject: no route accepts a user id, and every id in a path is checked against that owner
 * inside {@link HealthService}, which answers 404 rather than 403 for someone else's row so a
 * probe cannot confirm that the row exists.
 *
 * <p>Validation lives in {@code HealthService} rather than in bean-validation annotations here,
 * so a rule ("at most two decimal places", "the currency must match the profile") holds for every
 * caller and produces one stable error code, not two depending on which layer noticed first.
 *
 * <p>Nothing in this class logs a body, an amount, a balance or a description; the Health request
 * log is the shared {@code RequestIdFilter} correlation id and the status code, nothing more.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    // ------------------------------------------------------------------ profile

    /**
     * 404 before onboarding: a Petrimonium account exists (the token proves it), but this user has
     * no Health profile yet, and the app reads that as "show onboarding" rather than inventing a
     * default country/currency for them.
     */
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile() {
        return healthService.getProfile(SecurityUtils.getCurrentUserEmail())
                .map(ProfileResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<ProfileResponse> saveProfile(@RequestBody ProfileRequest request) {
        return ResponseEntity.ok(ProfileResponse.from(
                healthService.saveProfile(SecurityUtils.getCurrentUserEmail(), request.toInput())));
    }

    // ----------------------------------------------------------------- accounts

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> listAccounts() {
        return ResponseEntity.ok(healthService.listAccounts(SecurityUtils.getCurrentUserEmail())
                .stream().map(AccountResponse::from).toList());
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(
                healthService.createAccount(SecurityUtils.getCurrentUserEmail(), request.toInput())));
    }

    @PutMapping("/accounts/{accountId}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable long accountId,
                                                         @RequestBody AccountRequest request) {
        return ResponseEntity.ok(AccountResponse.from(healthService.updateAccount(
                SecurityUtils.getCurrentUserEmail(), accountId, request.toInput())));
    }

    /** Archives — history, balances and every row referencing the account are preserved. */
    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<Void> archiveAccount(@PathVariable long accountId) {
        healthService.archiveAccount(SecurityUtils.getCurrentUserEmail(), accountId);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------- transactions

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> listTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        var filter = new HealthService.TransactionFilter(from, to, accountId, category, status);
        return ResponseEntity.ok(healthService.listTransactions(SecurityUtils.getCurrentUserEmail(), filter)
                .stream().map(TransactionResponse::from).toList());
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(@RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(
                healthService.createTransaction(SecurityUtils.getCurrentUserEmail(), request.toInput())));
    }

    @PutMapping("/transactions/{transactionId}")
    public ResponseEntity<TransactionResponse> updateTransaction(@PathVariable long transactionId,
                                                                 @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(TransactionResponse.from(healthService.updateTransaction(
                SecurityUtils.getCurrentUserEmail(), transactionId, request.toInput())));
    }

    /**
     * Confirming a receipt or a payment moves the existing row to REALIZED. It never writes a
     * second row, so the same expense cannot be counted twice, and repeating the call on an
     * already-confirmed entry returns it unchanged.
     */
    @PostMapping("/transactions/{transactionId}/confirm")
    public ResponseEntity<TransactionResponse> confirmTransaction(@PathVariable long transactionId) {
        return ResponseEntity.ok(TransactionResponse.from(
                healthService.confirmTransaction(SecurityUtils.getCurrentUserEmail(), transactionId)));
    }

    @DeleteMapping("/transactions/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable long transactionId) {
        healthService.deleteTransaction(SecurityUtils.getCurrentUserEmail(), transactionId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- transfers

    /** Between the user's own accounts: neither income nor expense, and atomic on both sides. */
    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> createTransfer(@RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TransferResponse.from(
                healthService.createTransfer(SecurityUtils.getCurrentUserEmail(), request.toInput())));
    }

    // -------------------------------------------------------------- recurrences

    @GetMapping("/recurrences")
    public ResponseEntity<List<RecurrenceResponse>> listRecurrences() {
        return ResponseEntity.ok(healthService.listRecurrences(SecurityUtils.getCurrentUserEmail())
                .stream().map(RecurrenceResponse::from).toList());
    }

    @PostMapping("/recurrences")
    public ResponseEntity<RecurrenceResponse> createRecurrence(@RequestBody RecurrenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(RecurrenceResponse.from(
                healthService.createRecurrence(SecurityUtils.getCurrentUserEmail(), request.toInput())));
    }

    /**
     * Edits the model and the still-planned occurrences from the current month onwards. Anything
     * already confirmed keeps the value it was confirmed with — editing a rent increase does not
     * rewrite the rent the user actually paid last month.
     */
    @PutMapping("/recurrences/{recurrenceId}")
    public ResponseEntity<RecurrenceResponse> updateRecurrence(@PathVariable long recurrenceId,
                                                               @RequestBody RecurrenceRequest request) {
        return ResponseEntity.ok(RecurrenceResponse.from(healthService.updateRecurrence(
                SecurityUtils.getCurrentUserEmail(), recurrenceId, request.toInput())));
    }

    @DeleteMapping("/recurrences/{recurrenceId}")
    public ResponseEntity<Void> deactivateRecurrence(@PathVariable long recurrenceId) {
        healthService.deactivateRecurrence(SecurityUtils.getCurrentUserEmail(), recurrenceId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------- cards and invoices

    @GetMapping("/cards")
    public ResponseEntity<List<CardResponse>> listCards() {
        return ResponseEntity.ok(healthService.listCards(SecurityUtils.getCurrentUserEmail())
                .stream().map(CardResponse::from).toList());
    }

    @PostMapping("/cards")
    public ResponseEntity<CardResponse> createCard(@RequestBody CardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CardResponse.from(
                healthService.createCard(SecurityUtils.getCurrentUserEmail(), request.toInput())));
    }

    @PutMapping("/cards/{cardId}")
    public ResponseEntity<CardResponse> updateCard(@PathVariable long cardId,
                                                   @RequestBody CardRequest request) {
        return ResponseEntity.ok(CardResponse.from(healthService.updateCard(
                SecurityUtils.getCurrentUserEmail(), cardId, request.toInput())));
    }

    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Void> archiveCard(@PathVariable long cardId) {
        healthService.archiveCard(SecurityUtils.getCurrentUserEmail(), cardId);
        return ResponseEntity.noContent().build();
    }

    /**
     * A purchase, in one or many installments, distributed over invoices by the card's own closing
     * day. The installments always sum to exactly the purchase total.
     */
    @PostMapping("/cards/{cardId}/purchases")
    public ResponseEntity<PurchaseResponse> createPurchase(@PathVariable long cardId,
                                                           @RequestBody PurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseResponse.from(
                healthService.createPurchase(SecurityUtils.getCurrentUserEmail(), cardId, request.toInput())));
    }

    @GetMapping("/cards/{cardId}/invoices")
    public ResponseEntity<List<InvoiceResponse>> listInvoices(@PathVariable long cardId) {
        return ResponseEntity.ok(healthService.listInvoices(SecurityUtils.getCurrentUserEmail(), cardId)
                .stream().map(InvoiceResponse::from).toList());
    }

    /**
     * Paying an invoice moves money out of the chosen account and closes the invoice. It is not a
     * new expense — the spend was already recognised by the installments on that invoice.
     */
    @PostMapping("/cards/invoices/{invoiceId}/pay")
    public ResponseEntity<InvoiceResponse> payInvoice(@PathVariable long invoiceId,
                                                      @RequestBody InvoicePaymentRequest request) {
        return ResponseEntity.ok(InvoiceResponse.from(
                healthService.payInvoice(SecurityUtils.getCurrentUserEmail(), invoiceId, request.toInput())));
    }

    // ------------------------------------------------------------------ summary

    @GetMapping("/summary")
    public ResponseEntity<SummaryResponse> summary(@RequestParam(required = false) String month) {
        YearMonth parsed = parseMonth(month);
        return ResponseEntity.ok(SummaryResponse.from(
                healthService.summary(SecurityUtils.getCurrentUserEmail(), parsed)));
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
