package com.jf.PetApp.application.auth.port;

/**
 * Identity carried by a verified Google Sign-In ID token.
 *
 * @param sub Google's stable, unique identifier for the account.
 */
public record GoogleUserInfo(String sub, String email, String name) {
}
