package com.jf.PetApp.application.health.dto;

import static com.jf.PetApp.core.domain.health.HealthModels.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The inputs Health's use cases accept and the composite views they return.
 *
 * Kept in one holder for the same reason HealthModels and HealthApiDtos are:
 * this slice is a single bounded context whose records only ever travel
 * together.
 */
public final class HealthCommands {

    private HealthCommands() {
    }

    public record ProfileInput(String countryCode, String primaryCurrency, String localeTag) {}
    public record ProfileView(Profile profile, boolean currencyChangeAllowed) {}
    public record AccountInput(String name, String type, String initialBalance,
                               LocalDate balanceReferenceDate, String currency, String idempotencyKey) {}
    public record AccountView(Account account, BigDecimal currentBalance) {}
    public record TransactionInput(long accountId, String type, String status, String amount,
                                   String currency, String description, String category,
                                   LocalDate date, String idempotencyKey) {}
    public record TransactionFilter(LocalDate from, LocalDate to, Long accountId,
                                    String category, String status) {}
    public record TransferInput(long fromAccountId, long toAccountId, String amount, String currency,
                                LocalDate date, String description, String idempotencyKey) {}
    public record TransferView(Transfer transfer, Transaction outTransaction, Transaction inTransaction) {}
    public record RecurrenceInput(long accountId, String type, String amount, String currency,
                                  String description, String category, int dayOfMonth,
                                  LocalDate startDate, LocalDate endDate, String idempotencyKey) {}
    public record CardInput(String name, String currency, int closingDay, int dueDay, String idempotencyKey) {}
    public record PurchaseInput(String amount, String currency, String description, String category,
                                LocalDate purchaseDate, int installmentCount, String idempotencyKey) {}
    public record InvoicePaymentInput(long accountId, String currency, LocalDate paymentDate,
                                      String idempotencyKey) {}
}
