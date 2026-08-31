package com.jf.PetApp.application.user.port;

/**
 * Wipes accumulated progress for accounts that must always start fresh on
 * every login (e.g. the admin2 demo/onboarding account). Implementations
 * decide which usernames qualify; callers just invoke this unconditionally
 * on every successful login.
 */
public interface DemoAccountResetPort {
    void resetIfDemoAccount(String username);
}
