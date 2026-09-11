package com.jf.PetApp.core.domain;

import com.jf.PetApp.core.domain.enums.AssetOrigin;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single investment lot, independent of how it's persisted. {@code id} is
 * {@code null} for a lot that hasn't been saved yet (e.g. built from a
 * registration command before the adapter assigns a database id).
 *
 * <p>{@code quantity}/{@code purchasePrice} are {@link BigDecimal} — never
 * {@code double} — since this is real money: see
 * docs/BACKEND_MODULE_PLAN.md §12 for the precision/scale convention
 * (quantity scale 6 for fractional shares, price scale 2).</p>
 *
 * <p>{@code currency} and {@code origin} are never supplied by a client —
 * every write path (create/update/configure) is manual entry against the
 * only market Wallet supports today (B3, BRL), so the 7-arg constructor
 * below stamps both invariants automatically. They exist as real fields
 * (not implied/hardcoded downstream) so the record honestly states what it
 * represents, and so a future broker-sync path has somewhere to record a
 * different {@link AssetOrigin} without another migration.</p>
 */
public record Investment(
        Integer id,
        String userEmail,
        String name,
        BigDecimal quantity,
        BigDecimal purchasePrice,
        LocalDate purchaseDate,
        InvestmentType type,
        String currency,
        AssetOrigin origin
) {
    public static final String DEFAULT_CURRENCY = "BRL";

    public Investment(Integer id, String userEmail, String name, BigDecimal quantity, BigDecimal purchasePrice,
                       LocalDate purchaseDate, InvestmentType type) {
        this(id, userEmail, name, quantity, purchasePrice, purchaseDate, type, DEFAULT_CURRENCY, AssetOrigin.MANUAL);
    }
}
