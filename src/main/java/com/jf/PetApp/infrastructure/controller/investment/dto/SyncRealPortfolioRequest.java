package com.jf.PetApp.infrastructure.controller.investment.dto;

/**
 * Both fields optional: {@code externalAccountReference} is meaningless
 * until a real provider integration exists to resolve it against;
 * {@code idempotencyKey} is server-generated when absent (mirrors
 * simulated_portfolio's order-placement contract).
 */
public record SyncRealPortfolioRequest(
        String externalAccountReference,
        String idempotencyKey
) {
}
