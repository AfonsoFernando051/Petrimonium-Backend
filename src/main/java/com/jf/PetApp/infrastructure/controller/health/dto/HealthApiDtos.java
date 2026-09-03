package com.jf.PetApp.infrastructure.controller.health.dto;

import static com.jf.PetApp.core.domain.health.HealthModels.*;

import com.jf.PetApp.application.health.HealthService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * HTTP shapes for {@code /api/v1/health/**}, kept in one holder for the same reason
 * {@link com.jf.PetApp.core.domain.health.HealthModels} is: this slice is a single bounded
 * context whose records only ever travel together.
 *
 * <p>Two conversions matter and are deliberate, both documented in the Health app's
 * docs/API.md and docs/FINANCIAL_RULES.md:
 *
 * <ul>
 *   <li>Every monetary value crosses the wire as a decimal <em>string</em> ({@code "1234.50"}),
 *       never a JSON number — a mobile JSON decoder would otherwise widen it to a binary double
 *       and a cent could round away before the user ever sees it.
 *   <li>A month is {@code "YYYY-MM"} and a civil date {@code "YYYY-MM-DD"}, with no time and no
 *       zone: a bill due on the 31st is due on the 31st in Lisbon and in São Paulo alike.
 * </ul>
 */
public final class HealthApiDtos {

    private HealthApiDtos() {}

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    /** Scale-2 plain text: never scientific notation, never a bare {@code "12.5"} for 12.50. */
    private static String money(BigDecimal value) {
        return value == null ? null : value.setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString();
    }

    private static String monthText(YearMonth value) {
        return value == null ? null : value.format(MONTH);
    }

    // ---------------------------------------------------------------- requests

    public record ProfileRequest(String countryCode, String primaryCurrency, String localeTag) {
        public HealthService.ProfileInput toInput() {
            return new HealthService.ProfileInput(countryCode, primaryCurrency, localeTag);
        }
    }

    public record AccountRequest(String name, String type, String initialBalance,
                                 LocalDate balanceReferenceDate, String currency,
                                 String idempotencyKey) {
        public HealthService.AccountInput toInput() {
            return new HealthService.AccountInput(name, type, initialBalance, balanceReferenceDate,
                    currency, idempotencyKey);
        }
    }

    public record TransactionRequest(long accountId, String type, String status, String amount,
                                     String currency, String description, String category,
                                     LocalDate date, String idempotencyKey) {
        public HealthService.TransactionInput toInput() {
            return new HealthService.TransactionInput(accountId, type, status, amount, currency,
                    description, category, date, idempotencyKey);
        }
    }

    public record TransferRequest(long fromAccountId, long toAccountId, String amount, String currency,
                                  LocalDate date, String description, String idempotencyKey) {
        public HealthService.TransferInput toInput() {
            return new HealthService.TransferInput(fromAccountId, toAccountId, amount, currency,
                    date, description, idempotencyKey);
        }
    }

    public record RecurrenceRequest(long accountId, String type, String amount, String currency,
                                    String description, String category, int dayOfMonth,
                                    LocalDate startDate, LocalDate endDate, String idempotencyKey) {
        public HealthService.RecurrenceInput toInput() {
            return new HealthService.RecurrenceInput(accountId, type, amount, currency, description,
                    category, dayOfMonth, startDate, endDate, idempotencyKey);
        }
    }

    public record CardRequest(String name, String currency, int closingDay, int dueDay,
                              String idempotencyKey) {
        public HealthService.CardInput toInput() {
            return new HealthService.CardInput(name, currency, closingDay, dueDay, idempotencyKey);
        }
    }

    public record PurchaseRequest(String amount, String currency, String description, String category,
                                  LocalDate purchaseDate, int installmentCount, String idempotencyKey) {
        public HealthService.PurchaseInput toInput() {
            return new HealthService.PurchaseInput(amount, currency, description, category,
                    purchaseDate, installmentCount, idempotencyKey);
        }
    }

    public record InvoicePaymentRequest(long accountId, String currency, LocalDate paymentDate,
                                        String idempotencyKey) {
        public HealthService.InvoicePaymentInput toInput() {
            return new HealthService.InvoicePaymentInput(accountId, currency, paymentDate, idempotencyKey);
        }
    }

    // --------------------------------------------------------------- responses

    public record ProfileResponse(String countryCode, String primaryCurrency, String localeTag,
                                  boolean currencyChangeAllowed) {
        public static ProfileResponse from(HealthService.ProfileView view) {
            return new ProfileResponse(view.profile().countryCode().name(),
                    view.profile().primaryCurrency().name(), view.profile().localeTag(),
                    view.currencyChangeAllowed());
        }
    }

    public record AccountResponse(long id, String name, String type, String initialBalance,
                                  LocalDate balanceReferenceDate, String currentBalance,
                                  String currency, boolean archived) {
        public static AccountResponse from(HealthService.AccountView view) {
            Account account = view.account();
            return new AccountResponse(account.id(), account.name(), account.type().name(),
                    money(account.initialBalance()), account.balanceReferenceDate(),
                    money(view.currentBalance()), account.currency().name(), account.archived());
        }
    }

    /**
     * {@code source} and {@code recurrenceId}/{@code transferId}/{@code invoiceId} are exposed so
     * the app can tell a hand-entered row from one the backend generated (a recurrence occurrence,
     * a transfer leg, an invoice payment) and refuse to offer "edit" on the latter. They are also
     * where a future bank import identifies itself; nothing here is deduplicated by date+amount.
     */
    public record TransactionResponse(long id, long accountId, String type, String status,
                                      String amount, String currency, String description,
                                      String category, LocalDate date, String source,
                                      Long transferId, Long recurrenceId, Long invoiceId) {
        public static TransactionResponse from(Transaction tx) {
            return new TransactionResponse(tx.id(), tx.accountId(), tx.type().name(), tx.status().name(),
                    money(tx.amount()), tx.currency().name(), tx.description(), tx.category(),
                    tx.date(), tx.source().name(), tx.transferId(), tx.recurrenceId(), tx.invoiceId());
        }
    }

    /** One transfer, plus the two legs it produced — never two independent income/expense rows. */
    public record TransferResponse(long id, long fromAccountId, long toAccountId, String amount,
                                   String currency, LocalDate date, String description,
                                   TransactionResponse outLeg, TransactionResponse inLeg) {
        public static TransferResponse from(HealthService.TransferView view) {
            Transfer transfer = view.transfer();
            return new TransferResponse(transfer.id(), transfer.fromAccountId(), transfer.toAccountId(),
                    money(transfer.amount()), transfer.currency().name(), transfer.date(),
                    transfer.description(), TransactionResponse.from(view.outTransaction()),
                    TransactionResponse.from(view.inTransaction()));
        }
    }

    public record RecurrenceResponse(long id, long accountId, String type, String amount,
                                     String currency, String description, String category,
                                     int dayOfMonth, LocalDate startDate, LocalDate endDate,
                                     boolean active) {
        public static RecurrenceResponse from(Recurrence recurrence) {
            return new RecurrenceResponse(recurrence.id(), recurrence.accountId(), recurrence.type().name(),
                    money(recurrence.amount()), recurrence.currency().name(), recurrence.description(),
                    recurrence.category(), recurrence.dayOfMonth(), recurrence.startDate(),
                    recurrence.endDate(), recurrence.active());
        }
    }

    /** Credit limit is deliberately absent: it is not money the user has. */
    public record CardResponse(long id, String name, String currency, int closingDay, int dueDay,
                               boolean archived) {
        public static CardResponse from(Card card) {
            return new CardResponse(card.id(), card.name(), card.currency().name(), card.closingDay(),
                    card.dueDay(), card.archived());
        }
    }

    public record InstallmentResponse(long id, long purchaseId, long invoiceId, int installmentNumber,
                                      int installmentCount, String amount, String currency) {
        public static InstallmentResponse from(Installment installment) {
            return new InstallmentResponse(installment.id(), installment.purchaseId(), installment.invoiceId(),
                    installment.installmentNumber(), installment.installmentCount(),
                    money(installment.amount()), installment.currency().name());
        }
    }

    public record PurchaseResponse(long id, long cardId, String totalAmount, String currency,
                                   String description, String category, LocalDate purchaseDate,
                                   int installmentCount, List<InstallmentResponse> installments) {
        public static PurchaseResponse from(PurchaseWithInstallments result) {
            Purchase purchase = result.purchase();
            return new PurchaseResponse(purchase.id(), purchase.cardId(), money(purchase.totalAmount()),
                    purchase.currency().name(), purchase.description(), purchase.category(),
                    purchase.purchaseDate(), purchase.installmentCount(),
                    result.installments().stream().map(InstallmentResponse::from).toList());
        }
    }

    /** {@code amount} is the sum of the invoice's installments — the card's spend, recognised once. */
    public record InvoiceResponse(long id, long cardId, String currency, String cycleMonth,
                                 LocalDate closingDate, LocalDate dueDate, String status,
                                 boolean paid, LocalDate paidAt, String amount) {
        public static InvoiceResponse from(InvoiceWithTotal result) {
            Invoice invoice = result.invoice();
            return new InvoiceResponse(invoice.id(), invoice.cardId(), invoice.currency().name(),
                    monthText(invoice.cycleMonth()), invoice.closingDate(), invoice.dueDate(),
                    invoice.status().name(), invoice.status() == InvoiceStatus.PAID, invoice.paidAt(),
                    money(result.total()));
        }
    }

    public record CategoryAmountResponse(String category, String amount) {}

    public record UpcomingResponse(String kind, long id, String description, LocalDate date, String amount) {}

    /**
     * Three distinct numbers the app must never conflate: {@code currentBalance} is realised cash,
     * {@code monthResult} is the month's income minus expenses, and {@code projectedEndBalance} is
     * an estimate built from what is currently registered — planned entries and open invoices — not
     * a promise. Card spend is recognised through installments in {@code realizedExpenses}; an
     * invoice payment is not counted a second time.
     */
    public record SummaryResponse(String month, String currency, String currentBalance,
                                  String realizedIncome, String realizedExpenses,
                                  String plannedIncome, String plannedExpenses,
                                  String openCardInvoices, String monthResult,
                                  String projectedEndBalance,
                                  List<CategoryAmountResponse> expensesByCategory,
                                  List<UpcomingResponse> upcoming) {
        public static SummaryResponse from(MonthlySummary summary) {
            return new SummaryResponse(monthText(summary.month()), summary.currency().name(),
                    money(summary.currentBalance()), money(summary.realizedIncome()),
                    money(summary.realizedExpenses()), money(summary.plannedIncome()),
                    money(summary.plannedExpenses()), money(summary.openCardInvoices()),
                    money(summary.monthResult()), money(summary.projectedEndBalance()),
                    summary.expensesByCategory().stream()
                            .map(c -> new CategoryAmountResponse(c.category(), money(c.amount()))).toList(),
                    summary.upcoming().stream()
                            .map(u -> new UpcomingResponse(u.kind(), u.id(), u.description(), u.date(),
                                    money(u.amount()))).toList());
        }
    }
}
