package com.jf.PetApp.infrastructure.controller.health;

import static com.jf.PetApp.application.health.dto.HealthCommands.*;

import com.jf.PetApp.application.health.usecase.ArchiveHealthAccountUseCase;
import com.jf.PetApp.application.health.usecase.ArchiveHealthCardUseCase;
import com.jf.PetApp.application.health.usecase.ConfirmHealthTransactionUseCase;
import com.jf.PetApp.application.health.usecase.CreateHealthAccountUseCase;
import com.jf.PetApp.application.health.usecase.CreateHealthCardUseCase;
import com.jf.PetApp.application.health.usecase.CreateHealthPurchaseUseCase;
import com.jf.PetApp.application.health.usecase.CreateHealthRecurrenceUseCase;
import com.jf.PetApp.application.health.usecase.CreateHealthTransactionUseCase;
import com.jf.PetApp.application.health.usecase.CreateHealthTransferUseCase;
import com.jf.PetApp.application.health.usecase.DeactivateHealthRecurrenceUseCase;
import com.jf.PetApp.application.health.usecase.DeleteHealthTransactionUseCase;
import com.jf.PetApp.application.health.usecase.GetHealthProfileUseCase;
import com.jf.PetApp.application.health.usecase.GetHealthSummaryUseCase;
import com.jf.PetApp.application.health.usecase.ListHealthAccountsUseCase;
import com.jf.PetApp.application.health.usecase.ListHealthCardsUseCase;
import com.jf.PetApp.application.health.usecase.ListHealthInvoicesUseCase;
import com.jf.PetApp.application.health.usecase.ListHealthRecurrencesUseCase;
import com.jf.PetApp.application.health.usecase.ListHealthTransactionsUseCase;
import com.jf.PetApp.application.health.usecase.PayHealthInvoiceUseCase;
import com.jf.PetApp.application.health.usecase.SaveHealthProfileUseCase;
import com.jf.PetApp.application.health.usecase.UpdateHealthAccountUseCase;
import com.jf.PetApp.application.health.usecase.UpdateHealthCardUseCase;
import com.jf.PetApp.application.health.usecase.UpdateHealthRecurrenceUseCase;
import com.jf.PetApp.application.health.usecase.UpdateHealthTransactionUseCase;
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
 * inside the Health use cases, which answers 404 rather than 403 for someone else's row so a
 * probe cannot confirm that the row exists.
 *
 * <p>Validation lives in the Health use cases rather than in bean-validation annotations here,
 * so a rule ("at most two decimal places", "the currency must match the profile") holds for every
 * caller and produces one stable error code, not two depending on which layer noticed first.
 *
 * <p>Nothing in this class logs a body, an amount, a balance or a description; the Health request
 * log is the shared {@code RequestIdFilter} correlation id and the status code, nothing more.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final GetHealthProfileUseCase getHealthProfileUseCase;
    private final SaveHealthProfileUseCase saveHealthProfileUseCase;
    private final ListHealthAccountsUseCase listHealthAccountsUseCase;
    private final CreateHealthAccountUseCase createHealthAccountUseCase;
    private final UpdateHealthAccountUseCase updateHealthAccountUseCase;
    private final ArchiveHealthAccountUseCase archiveHealthAccountUseCase;
    private final ListHealthTransactionsUseCase listHealthTransactionsUseCase;
    private final CreateHealthTransactionUseCase createHealthTransactionUseCase;
    private final UpdateHealthTransactionUseCase updateHealthTransactionUseCase;
    private final ConfirmHealthTransactionUseCase confirmHealthTransactionUseCase;
    private final DeleteHealthTransactionUseCase deleteHealthTransactionUseCase;
    private final CreateHealthTransferUseCase createHealthTransferUseCase;
    private final ListHealthRecurrencesUseCase listHealthRecurrencesUseCase;
    private final CreateHealthRecurrenceUseCase createHealthRecurrenceUseCase;
    private final UpdateHealthRecurrenceUseCase updateHealthRecurrenceUseCase;
    private final DeactivateHealthRecurrenceUseCase deactivateHealthRecurrenceUseCase;
    private final ListHealthCardsUseCase listHealthCardsUseCase;
    private final CreateHealthCardUseCase createHealthCardUseCase;
    private final UpdateHealthCardUseCase updateHealthCardUseCase;
    private final ArchiveHealthCardUseCase archiveHealthCardUseCase;
    private final CreateHealthPurchaseUseCase createHealthPurchaseUseCase;
    private final ListHealthInvoicesUseCase listHealthInvoicesUseCase;
    private final PayHealthInvoiceUseCase payHealthInvoiceUseCase;
    private final GetHealthSummaryUseCase getHealthSummaryUseCase;

    public HealthController(
            GetHealthProfileUseCase getHealthProfileUseCase,
            SaveHealthProfileUseCase saveHealthProfileUseCase,
            ListHealthAccountsUseCase listHealthAccountsUseCase,
            CreateHealthAccountUseCase createHealthAccountUseCase,
            UpdateHealthAccountUseCase updateHealthAccountUseCase,
            ArchiveHealthAccountUseCase archiveHealthAccountUseCase,
            ListHealthTransactionsUseCase listHealthTransactionsUseCase,
            CreateHealthTransactionUseCase createHealthTransactionUseCase,
            UpdateHealthTransactionUseCase updateHealthTransactionUseCase,
            ConfirmHealthTransactionUseCase confirmHealthTransactionUseCase,
            DeleteHealthTransactionUseCase deleteHealthTransactionUseCase,
            CreateHealthTransferUseCase createHealthTransferUseCase,
            ListHealthRecurrencesUseCase listHealthRecurrencesUseCase,
            CreateHealthRecurrenceUseCase createHealthRecurrenceUseCase,
            UpdateHealthRecurrenceUseCase updateHealthRecurrenceUseCase,
            DeactivateHealthRecurrenceUseCase deactivateHealthRecurrenceUseCase,
            ListHealthCardsUseCase listHealthCardsUseCase,
            CreateHealthCardUseCase createHealthCardUseCase,
            UpdateHealthCardUseCase updateHealthCardUseCase,
            ArchiveHealthCardUseCase archiveHealthCardUseCase,
            CreateHealthPurchaseUseCase createHealthPurchaseUseCase,
            ListHealthInvoicesUseCase listHealthInvoicesUseCase,
            PayHealthInvoiceUseCase payHealthInvoiceUseCase,
            GetHealthSummaryUseCase getHealthSummaryUseCase
    ) {
        this.getHealthProfileUseCase = getHealthProfileUseCase;
        this.saveHealthProfileUseCase = saveHealthProfileUseCase;
        this.listHealthAccountsUseCase = listHealthAccountsUseCase;
        this.createHealthAccountUseCase = createHealthAccountUseCase;
        this.updateHealthAccountUseCase = updateHealthAccountUseCase;
        this.archiveHealthAccountUseCase = archiveHealthAccountUseCase;
        this.listHealthTransactionsUseCase = listHealthTransactionsUseCase;
        this.createHealthTransactionUseCase = createHealthTransactionUseCase;
        this.updateHealthTransactionUseCase = updateHealthTransactionUseCase;
        this.confirmHealthTransactionUseCase = confirmHealthTransactionUseCase;
        this.deleteHealthTransactionUseCase = deleteHealthTransactionUseCase;
        this.createHealthTransferUseCase = createHealthTransferUseCase;
        this.listHealthRecurrencesUseCase = listHealthRecurrencesUseCase;
        this.createHealthRecurrenceUseCase = createHealthRecurrenceUseCase;
        this.updateHealthRecurrenceUseCase = updateHealthRecurrenceUseCase;
        this.deactivateHealthRecurrenceUseCase = deactivateHealthRecurrenceUseCase;
        this.listHealthCardsUseCase = listHealthCardsUseCase;
        this.createHealthCardUseCase = createHealthCardUseCase;
        this.updateHealthCardUseCase = updateHealthCardUseCase;
        this.archiveHealthCardUseCase = archiveHealthCardUseCase;
        this.createHealthPurchaseUseCase = createHealthPurchaseUseCase;
        this.listHealthInvoicesUseCase = listHealthInvoicesUseCase;
        this.payHealthInvoiceUseCase = payHealthInvoiceUseCase;
        this.getHealthSummaryUseCase = getHealthSummaryUseCase;
    }

    // ------------------------------------------------------------------ profile

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

    // ----------------------------------------------------------------- accounts

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> listAccounts() {
        return ResponseEntity.ok(listHealthAccountsUseCase.execute(SecurityUtils.getCurrentUserEmail())
                .stream().map(AccountResponse::from).toList());
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@RequestBody AccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(
                createHealthAccountUseCase.execute(SecurityUtils.getCurrentUserEmail(), request.toInput())));
    }

    @PutMapping("/accounts/{accountId}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable long accountId,
                                                         @RequestBody AccountRequest request) {
        return ResponseEntity.ok(AccountResponse.from(updateHealthAccountUseCase.execute(
                SecurityUtils.getCurrentUserEmail(), accountId, request.toInput())));
    }

    /** Archives — history, balances and every row referencing the account are preserved. */
    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<Void> archiveAccount(@PathVariable long accountId) {
        archiveHealthAccountUseCase.execute(SecurityUtils.getCurrentUserEmail(), accountId);
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
        var filter = new TransactionFilter(from, to, accountId, category, status);
        return ResponseEntity.ok(listHealthTransactionsUseCase.execute(SecurityUtils.getCurrentUserEmail(), filter)
                .stream().map(TransactionResponse::from).toList());
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(@RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionResponse.from(
                createHealthTransactionUseCase.execute(SecurityUtils.getCurrentUserEmail(), request.toInput())));
    }

    @PutMapping("/transactions/{transactionId}")
    public ResponseEntity<TransactionResponse> updateTransaction(@PathVariable long transactionId,
                                                                 @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(TransactionResponse.from(updateHealthTransactionUseCase.execute(
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
                confirmHealthTransactionUseCase.execute(SecurityUtils.getCurrentUserEmail(), transactionId)));
    }

    @DeleteMapping("/transactions/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable long transactionId) {
        deleteHealthTransactionUseCase.execute(SecurityUtils.getCurrentUserEmail(), transactionId);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- transfers

    /** Between the user's own accounts: neither income nor expense, and atomic on both sides. */
    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> createTransfer(@RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TransferResponse.from(
                createHealthTransferUseCase.execute(SecurityUtils.getCurrentUserEmail(), request.toInput())));
    }

    // -------------------------------------------------------------- recurrences

    @GetMapping("/recurrences")
    public ResponseEntity<List<RecurrenceResponse>> listRecurrences() {
        return ResponseEntity.ok(listHealthRecurrencesUseCase.execute(SecurityUtils.getCurrentUserEmail())
                .stream().map(RecurrenceResponse::from).toList());
    }

    @PostMapping("/recurrences")
    public ResponseEntity<RecurrenceResponse> createRecurrence(@RequestBody RecurrenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(RecurrenceResponse.from(
                createHealthRecurrenceUseCase.execute(SecurityUtils.getCurrentUserEmail(), request.toInput())));
    }

    /**
     * Edits the model and the still-planned occurrences from the current month onwards. Anything
     * already confirmed keeps the value it was confirmed with — editing a rent increase does not
     * rewrite the rent the user actually paid last month.
     */
    @PutMapping("/recurrences/{recurrenceId}")
    public ResponseEntity<RecurrenceResponse> updateRecurrence(@PathVariable long recurrenceId,
                                                               @RequestBody RecurrenceRequest request) {
        return ResponseEntity.ok(RecurrenceResponse.from(updateHealthRecurrenceUseCase.execute(
                SecurityUtils.getCurrentUserEmail(), recurrenceId, request.toInput())));
    }

    @DeleteMapping("/recurrences/{recurrenceId}")
    public ResponseEntity<Void> deactivateRecurrence(@PathVariable long recurrenceId) {
        deactivateHealthRecurrenceUseCase.execute(SecurityUtils.getCurrentUserEmail(), recurrenceId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------- cards and invoices

    @GetMapping("/cards")
    public ResponseEntity<List<CardResponse>> listCards() {
        return ResponseEntity.ok(listHealthCardsUseCase.execute(SecurityUtils.getCurrentUserEmail())
                .stream().map(CardResponse::from).toList());
    }

    @PostMapping("/cards")
    public ResponseEntity<CardResponse> createCard(@RequestBody CardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(CardResponse.from(
                createHealthCardUseCase.execute(SecurityUtils.getCurrentUserEmail(), request.toInput())));
    }

    @PutMapping("/cards/{cardId}")
    public ResponseEntity<CardResponse> updateCard(@PathVariable long cardId,
                                                   @RequestBody CardRequest request) {
        return ResponseEntity.ok(CardResponse.from(updateHealthCardUseCase.execute(
                SecurityUtils.getCurrentUserEmail(), cardId, request.toInput())));
    }

    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Void> archiveCard(@PathVariable long cardId) {
        archiveHealthCardUseCase.execute(SecurityUtils.getCurrentUserEmail(), cardId);
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
                createHealthPurchaseUseCase.execute(SecurityUtils.getCurrentUserEmail(), cardId, request.toInput())));
    }

    @GetMapping("/cards/{cardId}/invoices")
    public ResponseEntity<List<InvoiceResponse>> listInvoices(@PathVariable long cardId) {
        return ResponseEntity.ok(listHealthInvoicesUseCase.execute(SecurityUtils.getCurrentUserEmail(), cardId)
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
                payHealthInvoiceUseCase.execute(SecurityUtils.getCurrentUserEmail(), invoiceId, request.toInput())));
    }

    // ------------------------------------------------------------------ summary

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
