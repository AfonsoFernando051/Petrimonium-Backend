package com.jf.PetApp.application.health.port;

import static com.jf.PetApp.core.domain.health.HealthModels.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/** Database boundary for the Health bounded context. */
public interface HealthStore {

    Optional<Profile> findProfile(long userId);
    Optional<Profile> findProfileForUpdate(long userId);
    Profile createProfile(long userId, CountryCode country, CurrencyCode currency, String localeTag);
    Profile updateProfile(long userId, CountryCode country, CurrencyCode currency, String localeTag);
    boolean hasFinancialData(long userId);

    List<Account> listAccounts(long userId);
    Optional<Account> findAccount(long userId, long accountId);
    Optional<Account> findAccountByIdempotencyKey(long userId, String key);
    Account createAccount(long userId, String name, AccountType type, BigDecimal initialBalance,
                          LocalDate referenceDate, CurrencyCode currency, String key);
    Account updateAccount(long userId, long accountId, String name, AccountType type,
                          BigDecimal initialBalance, LocalDate referenceDate, CurrencyCode currency);
    void archiveAccount(long userId, long accountId);
    BigDecimal accountBalance(long userId, Account account);

    List<Transaction> listTransactions(long userId);
    Optional<Transaction> findTransaction(long userId, long transactionId);
    Optional<Transaction> findTransactionByIdempotencyKey(long userId, String key);
    List<Transaction> findTransactionsByTransfer(long userId, long transferId);
    Transaction createTransaction(long userId, long accountId, EntryType type, EntryStatus status,
                                  BigDecimal amount, CurrencyCode currency, String description,
                                  String category, LocalDate date, RecordSource source,
                                  String externalProvider, String externalId, String key,
                                  Long transferId, Long recurrenceId, YearMonth recurrenceMonth,
                                  Long invoiceId);
    Transaction updateTransaction(long userId, long transactionId, long accountId, EntryType type,
                                  EntryStatus status, BigDecimal amount, CurrencyCode currency,
                                  String description, String category, LocalDate date);
    void softDeleteTransaction(long userId, long transactionId);
    void deletePlannedRecurrenceOccurrencesFrom(long userId, long recurrenceId, LocalDate from);

    Optional<Transfer> findTransferByIdempotencyKey(long userId, String key);
    Transfer createTransfer(long userId, long fromAccountId, long toAccountId, BigDecimal amount,
                            CurrencyCode currency, LocalDate date, String description, String key);

    List<Recurrence> listRecurrences(long userId);
    Optional<Recurrence> findRecurrence(long userId, long recurrenceId);
    Optional<Recurrence> findRecurrenceByIdempotencyKey(long userId, String key);
    Recurrence createRecurrence(long userId, long accountId, EntryType type, BigDecimal amount,
                                CurrencyCode currency, String description, String category,
                                int dayOfMonth, LocalDate startDate, LocalDate endDate, String key);
    Recurrence updateRecurrence(long userId, long recurrenceId, long accountId, EntryType type,
                                BigDecimal amount, CurrencyCode currency, String description,
                                String category, int dayOfMonth, LocalDate startDate, LocalDate endDate);
    void deactivateRecurrence(long userId, long recurrenceId);

    List<Card> listCards(long userId);
    Optional<Card> findCard(long userId, long cardId);
    Optional<Card> findCardByIdempotencyKey(long userId, String key);
    Card createCard(long userId, String name, CurrencyCode currency, int closingDay, int dueDay, String key);
    Card updateCard(long userId, long cardId, String name, CurrencyCode currency, int closingDay, int dueDay);
    void archiveCard(long userId, long cardId);

    Optional<Invoice> findInvoice(long userId, long invoiceId);
    Optional<Invoice> findInvoiceByCardAndCycle(long userId, long cardId, YearMonth cycleMonth);
    Invoice createInvoice(long userId, long cardId, CurrencyCode currency, YearMonth cycleMonth,
                          LocalDate closingDate, LocalDate dueDate);
    List<Invoice> listInvoices(long userId, long cardId);
    List<Invoice> listInvoices(long userId);
    BigDecimal invoiceTotal(long userId, long invoiceId);
    Invoice markInvoicePaid(long userId, long invoiceId, LocalDate paidAt, long transactionId);

    Optional<Purchase> findPurchaseByIdempotencyKey(long userId, String key);
    Purchase createPurchase(long userId, long cardId, BigDecimal totalAmount, CurrencyCode currency,
                            String description, String category, LocalDate purchaseDate,
                            int installmentCount, RecordSource source, String externalProvider,
                            String externalId, String key);
    Installment createInstallment(long userId, long purchaseId, long invoiceId,
                                  CurrencyCode currency, int number, int count, BigDecimal amount);
    List<Installment> listInstallmentsByPurchase(long userId, long purchaseId);
    List<Installment> listInstallmentsByInvoice(long userId, long invoiceId);
    List<Purchase> listPurchases(long userId);
}
