package com.jf.PetApp.core.domain.enums;

/**
 * Where a lot's {@code currentPrice} actually came from.
 *
 * <p>Exists because falling back to the purchase price is indistinguishable, downstream, from a
 * real quote that happens to equal it: both render as "0% gain". The user is then shown "your
 * position hasn't moved" when the truth is "we don't know what it's worth". This flag is what
 * lets the client tell those two apart (docs/BACKEND_MODULE_PLAN.md §12).
 *
 * <p>Deliberately describes the <em>price</em>, not the asset: the same ticker can be {@link #LIVE}
 * on one request and {@link #STALE_PURCHASE_PRICE} on the next when the provider is down.
 */
public enum PriceStatus {

    /** A real quote from the market-data provider. Gain/loss is meaningful. */
    LIVE,

    /**
     * The quote was unavailable (provider error, empty result, or no token) and {@code currentPrice}
     * fell back to the lot's purchase price. Gain/loss is <em>not</em> meaningful and must not be
     * presented as a real return.
     */
    STALE_PURCHASE_PRICE,

    /**
     * The asset class has no quote feed at all — fixed income has no ticker on the provider and no
     * accrual-based pricing model exists yet (see {@code GetPortfolioHoldingsUseCaseImpl}). Unlike
     * {@link #STALE_PURCHASE_PRICE} this is not a failure and will not resolve itself by retrying.
     */
    NOT_QUOTED
}
