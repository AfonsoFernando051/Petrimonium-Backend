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

import com.jf.PetApp.application.health.usecase.ConfirmHealthTransactionUseCase;
import com.jf.PetApp.application.health.usecase.CreateHealthTransactionUseCase;
import com.jf.PetApp.application.health.usecase.CreateHealthTransferUseCase;
import com.jf.PetApp.application.health.usecase.DeleteHealthTransactionUseCase;
import com.jf.PetApp.application.health.usecase.ListHealthTransactionsUseCase;
import com.jf.PetApp.application.health.usecase.UpdateHealthTransactionUseCase;

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
 * <p>This controller covers entries and transfers — every movement of money, planned or realized.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthTransactionController {

    private final ListHealthTransactionsUseCase listHealthTransactionsUseCase;
    private final CreateHealthTransactionUseCase createHealthTransactionUseCase;
    private final UpdateHealthTransactionUseCase updateHealthTransactionUseCase;
    private final ConfirmHealthTransactionUseCase confirmHealthTransactionUseCase;
    private final DeleteHealthTransactionUseCase deleteHealthTransactionUseCase;
    private final CreateHealthTransferUseCase createHealthTransferUseCase;

    public HealthTransactionController(
            ListHealthTransactionsUseCase listHealthTransactionsUseCase,
            CreateHealthTransactionUseCase createHealthTransactionUseCase,
            UpdateHealthTransactionUseCase updateHealthTransactionUseCase,
            ConfirmHealthTransactionUseCase confirmHealthTransactionUseCase,
            DeleteHealthTransactionUseCase deleteHealthTransactionUseCase,
            CreateHealthTransferUseCase createHealthTransferUseCase
    ) {
        this.listHealthTransactionsUseCase = listHealthTransactionsUseCase;
        this.createHealthTransactionUseCase = createHealthTransactionUseCase;
        this.updateHealthTransactionUseCase = updateHealthTransactionUseCase;
        this.confirmHealthTransactionUseCase = confirmHealthTransactionUseCase;
        this.deleteHealthTransactionUseCase = deleteHealthTransactionUseCase;
        this.createHealthTransferUseCase = createHealthTransferUseCase;
    }

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

    /** Between the user's own accounts: neither income nor expense, and atomic on both sides. */
    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> createTransfer(@RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TransferResponse.from(
                createHealthTransferUseCase.execute(SecurityUtils.getCurrentUserEmail(), request.toInput())));
    }
}
