package com.jf.PetApp.application.health.service;

import static com.jf.PetApp.core.domain.health.HealthModels.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/**
 * Input validation and normalization for Health's write paths, plus the
 * "is this request the same as the one that already used this idempotency
 * key" comparisons.
 *
 * Extracted from {@code HealthService} so that the use cases share one
 * definition of what a valid amount, category or day-of-month is. Every
 * method is a pure function of its arguments — no store, no framework.
 */
public final class HealthValidation {

    private HealthValidation() {
    }

    public static BigDecimal parseMoney(String raw, boolean positive, String field) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(field + " is required as a decimal string");
        }
        final BigDecimal parsed;
        try {
            parsed = new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be a decimal string");
        }
        BigDecimal stripped = parsed.stripTrailingZeros();
        if (stripped.scale() > 2) {
            throw new IllegalArgumentException(field + " must have at most 2 decimal places");
        }
        BigDecimal value = parsed.setScale(2, RoundingMode.UNNECESSARY);
        if (value.precision() > 19) {
            throw new IllegalArgumentException(field + " is too large");
        }
        if (positive && value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static <E extends Enum<E>> E enumValue(Class<E> type, String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported " + field + ": " + raw);
        }
    }

    public static EntryType publicEntryType(String raw) {
        EntryType type = enumValue(EntryType.class, raw, "type");
        if (type != EntryType.INCOME && type != EntryType.EXPENSE) {
            throw new IllegalArgumentException("type must be INCOME or EXPENSE");
        }
        return type;
    }

    public static String requireLocale(String locale) {
        if (!"pt-BR".equals(locale) && !"pt-PT".equals(locale)) {
            throw new IllegalArgumentException("localeTag must be 'pt-BR' or 'pt-PT'");
        }
        return locale;
    }

    public static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(field + " must have at most " + maxLength + " characters");
        }
        return trimmed;
    }

    public static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireText(value, field, maxLength);
    }

    public static LocalDate requireDate(LocalDate date, String field) {
        if (date == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return date;
    }

    public static int requireDay(int value, String field) {
        if (value < 1 || value > 31) {
            throw new IllegalArgumentException(field + " must be between 1 and 31");
        }
        return value;
    }

    public static String requireKey(String key) {
        return requireText(key, "idempotencyKey", 64);
    }

    public static String normalizeCategory(String category) {
        return category == null ? null : category.trim().toLowerCase(Locale.ROOT);
    }

    public static String categoryOrOther(String category) {
        return category == null || category.isBlank() ? "other" : category;
    }

    public static boolean sameAccount(Account a, String name, AccountType type, BigDecimal initial,
                                       LocalDate date, CurrencyCode currency) {
        return a.name().equals(name) && a.type() == type && a.initialBalance().compareTo(initial) == 0
                && a.balanceReferenceDate().equals(date) && a.currency() == currency;
    }

    public static boolean sameTransaction(Transaction t, long accountId, EntryType type, EntryStatus status,
                                           BigDecimal amount, CurrencyCode currency, String description,
                                           String category, LocalDate date) {
        return t.accountId() == accountId && t.type() == type && t.status() == status
                && t.amount().compareTo(amount) == 0 && t.currency() == currency
                && t.description().equals(description) && Objects.equals(t.category(), category)
                && t.date().equals(date) && t.deletedAt() == null;
    }

    public static boolean sameRecurrence(Recurrence r, long accountId, EntryType type, BigDecimal amount,
                                          CurrencyCode currency, String description, String category, int day,
                                          LocalDate start, LocalDate end) {
        return r.accountId() == accountId && r.type() == type && r.amount().compareTo(amount) == 0
                && r.currency() == currency && r.description().equals(description)
                && Objects.equals(r.category(), category) && r.dayOfMonth() == day
                && r.startDate().equals(start) && Objects.equals(r.endDate(), end);
    }
}
