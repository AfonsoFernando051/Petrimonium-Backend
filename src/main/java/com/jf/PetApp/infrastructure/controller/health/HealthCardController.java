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

import com.jf.PetApp.application.health.usecase.ArchiveHealthCardUseCase;
import com.jf.PetApp.application.health.usecase.CreateHealthCardUseCase;
import com.jf.PetApp.application.health.usecase.CreateHealthPurchaseUseCase;
import com.jf.PetApp.application.health.usecase.ListHealthCardsUseCase;
import com.jf.PetApp.application.health.usecase.ListHealthInvoicesUseCase;
import com.jf.PetApp.application.health.usecase.PayHealthInvoiceUseCase;
import com.jf.PetApp.application.health.usecase.UpdateHealthCardUseCase;

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
 * <p>This controller covers credit cards, their installment purchases and their invoices.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthCardController {

    private final ListHealthCardsUseCase listHealthCardsUseCase;
    private final CreateHealthCardUseCase createHealthCardUseCase;
    private final UpdateHealthCardUseCase updateHealthCardUseCase;
    private final ArchiveHealthCardUseCase archiveHealthCardUseCase;
    private final CreateHealthPurchaseUseCase createHealthPurchaseUseCase;
    private final ListHealthInvoicesUseCase listHealthInvoicesUseCase;
    private final PayHealthInvoiceUseCase payHealthInvoiceUseCase;

    public HealthCardController(
            ListHealthCardsUseCase listHealthCardsUseCase,
            CreateHealthCardUseCase createHealthCardUseCase,
            UpdateHealthCardUseCase updateHealthCardUseCase,
            ArchiveHealthCardUseCase archiveHealthCardUseCase,
            CreateHealthPurchaseUseCase createHealthPurchaseUseCase,
            ListHealthInvoicesUseCase listHealthInvoicesUseCase,
            PayHealthInvoiceUseCase payHealthInvoiceUseCase
    ) {
        this.listHealthCardsUseCase = listHealthCardsUseCase;
        this.createHealthCardUseCase = createHealthCardUseCase;
        this.updateHealthCardUseCase = updateHealthCardUseCase;
        this.archiveHealthCardUseCase = archiveHealthCardUseCase;
        this.createHealthPurchaseUseCase = createHealthPurchaseUseCase;
        this.listHealthInvoicesUseCase = listHealthInvoicesUseCase;
        this.payHealthInvoiceUseCase = payHealthInvoiceUseCase;
    }

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
}
