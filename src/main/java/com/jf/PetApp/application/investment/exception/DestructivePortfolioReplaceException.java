package com.jf.PetApp.application.investment.exception;

/**
 * Thrown when a configure-investments request would remove lots the user
 * currently holds without the caller explicitly acknowledging the replacement.
 *
 * <p>{@code POST /api/investments/configure} replaces the whole portfolio
 * rather than appending, so a client that submits a partial list — because it
 * failed to load the current holdings first, for instance — silently destroys
 * real financial data. The client-side guard for that lives in the Wallet app,
 * but a guard there only protects clients that have been updated; this one
 * protects every caller, including app versions already installed on devices.</p>
 *
 * <p>Deliberately scoped to <em>reductions</em>: adding assets is the common
 * path and never needs acknowledgment, so an older client keeps working for it.
 * Only a request that would end up with fewer lots than the user has today has
 * to say it means it.</p>
 */
public class DestructivePortfolioReplaceException extends RuntimeException {

    private final int currentLotCount;
    private final int submittedLotCount;

    public DestructivePortfolioReplaceException(int currentLotCount, int submittedLotCount) {
        super(String.format(
                "This request would replace %d existing investment(s) with %d. "
                        + "Resend with confirmReplace=true if that is intended.",
                currentLotCount, submittedLotCount));
        this.currentLotCount = currentLotCount;
        this.submittedLotCount = submittedLotCount;
    }

    public int currentLotCount() {
        return currentLotCount;
    }

    public int submittedLotCount() {
        return submittedLotCount;
    }
}
