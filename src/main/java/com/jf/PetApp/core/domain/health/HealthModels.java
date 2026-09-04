package com.jf.PetApp.core.domain.health;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Persistence-independent values used by the Health application service.
 * Monetary values are BigDecimal at scale 2; the HTTP boundary serializes
 * them as decimal strings so a mobile JSON decoder never passes them through
 * binary floating point.
 */
public final class HealthModels {

    private HealthModels() {}

    public enum CountryCode { BR, PT }
    public enum CurrencyCode { BRL, EUR }
    public enum AccountType { CHECKING, SAVINGS, CASH, OTHER }
    public enum EntryType { INCOME, EXPENSE, TRANSFER_IN, TRANSFER_OUT, INVOICE_PAYMENT }
    public enum EntryStatus { PLANNED, REALIZED }
    public enum RecordSource { MANUAL, IMPORTED, SYSTEM }
    public enum InvoiceStatus { OPEN, PAID }

    public record Profile(
            long userId,
            CountryCode countryCode,
            CurrencyCode primaryCurrency,
            String localeTag,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record Account(
            long id,
            long userId,
            String name,
            AccountType type,
            BigDecimal initialBalance,
            LocalDate balanceReferenceDate,
            CurrencyCode currency,
            boolean archived,
            String idempotencyKey,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record Transaction(
            long id,
            long userId,
            long accountId,
            EntryType type,
            EntryStatus status,
            BigDecimal amount,
            CurrencyCode currency,
            String description,
            String category,
            LocalDate date,
            RecordSource source,
            String externalProvider,
            String externalId,
            String idempotencyKey,
            Long transferId,
            Long recurrenceId,
            YearMonth recurrenceMonth,
            Long invoiceId,
            Instant deletedAt,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record Transfer(
            long id,
            long userId,
            long fromAccountId,
            long toAccountId,
            BigDecimal amount,
            CurrencyCode currency,
            LocalDate date,
            String description,
            String idempotencyKey,
            Instant createdAt
    ) {}

    public record Recurrence(
            long id,
            long userId,
            long accountId,
            EntryType type,
            BigDecimal amount,
            CurrencyCode currency,
            String description,
            String category,
            int dayOfMonth,
            LocalDate startDate,
            LocalDate endDate,
            boolean active,
            String idempotencyKey,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record Card(
            long id,
            long userId,
            String name,
            CurrencyCode currency,
            int closingDay,
            int dueDay,
            boolean archived,
            String idempotencyKey,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record Invoice(
            long id,
            long userId,
            long cardId,
            CurrencyCode currency,
            YearMonth cycleMonth,
            LocalDate closingDate,
            LocalDate dueDate,
            InvoiceStatus status,
            LocalDate paidAt,
            Long paidTransactionId,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record Purchase(
            long id,
            long userId,
            long cardId,
            BigDecimal totalAmount,
            CurrencyCode currency,
            String description,
            String category,
            LocalDate purchaseDate,
            int installmentCount,
            RecordSource source,
            String externalProvider,
            String externalId,
            String idempotencyKey,
            Instant createdAt
    ) {}

    public record Installment(
            long id,
            long userId,
            long purchaseId,
            long invoiceId,
            CurrencyCode currency,
            int installmentNumber,
            int installmentCount,
            BigDecimal amount
    ) {}

    public record PurchaseWithInstallments(Purchase purchase, List<Installment> installments) {}

    public record InvoiceWithTotal(Invoice invoice, BigDecimal total) {}

    public record CategoryAmount(String category, BigDecimal amount) {}

    public record Upcoming(String kind, long id, String description, LocalDate date, BigDecimal amount) {}

    public record MonthlySummary(
            YearMonth month,
            CurrencyCode currency,
            BigDecimal currentBalance,
            BigDecimal realizedIncome,
            BigDecimal realizedExpenses,
            BigDecimal plannedIncome,
            BigDecimal plannedExpenses,
            BigDecimal openCardInvoices,
            BigDecimal monthResult,
            BigDecimal projectedEndBalance,
            List<CategoryAmount> expensesByCategory,
            List<Upcoming> upcoming
    ) {}
}
