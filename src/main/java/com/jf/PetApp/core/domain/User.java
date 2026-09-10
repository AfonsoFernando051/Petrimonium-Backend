package com.jf.PetApp.core.domain;

import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import com.jf.PetApp.core.domain.enums.AuthProviderEnum;
import com.jf.PetApp.core.domain.enums.RoleEnum;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {

	/**
	 * Id for pet
	 */
	Long id;

	/**
	 * Name for user
	 */
	String username;

	/**
	 * Name for user
	 */
	String email;

	/**
	 * Password for user
	 */
	String password;

	/**
	 * The user's pet
	 */
	Finance finance;

	/**
	 * The user's role
	 */
	RoleEnum role;

	/**
	 * The user's active status
	 */
	boolean isActive;

	/**
	 * Whether the user already answered the investor questionnaire.
	 */
	@Getter(AccessLevel.NONE)
	boolean hasAnsweredOnboarding;

	/**
	 * Computed investor profile based on the questionnaire answers.
	 */
	InvestorProfile investorProfile;

	/**
	 * The user's preferred UI/content language (e.g. "pt", "en", "es"). Defaults to "pt".
	 */
	String preferredLanguage = "pt";

	/**
	 * ISO 3166-1 alpha-2 do país da conta ("BR", "PT"). Nulo enquanto o
	 * utilizador não escolher — não se assume país a partir do dispositivo.
	 */
	String countryCode;

	/**
	 * How this user authenticates. LOCAL users have a password; GOOGLE users
	 * authenticate via a verified Google ID token and have none.
	 */
	AuthProviderEnum provider = AuthProviderEnum.LOCAL;

	/**
	 * Google's `sub` claim, uniquely identifying the Google account. Null for LOCAL users.
	 */
	String providerId;

	public boolean hasAnsweredOnboarding() {
		return hasAnsweredOnboarding;
	}

	public static User create(String username, String email, String password, RoleEnum role) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword(password);
		user.setRole(role);
		user.setActive(true);
		user.setHasAnsweredOnboarding(false);
		user.setInvestorProfile(null);
		user.setPreferredLanguage("pt");
		user.setProvider(AuthProviderEnum.LOCAL);
		return user;
	}

	public static User createFromGoogle(String username, String email, String providerId, RoleEnum role) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword(null);
		user.setRole(role);
		user.setActive(true);
		user.setHasAnsweredOnboarding(false);
		user.setInvestorProfile(null);
		user.setPreferredLanguage("pt");
		user.setProvider(AuthProviderEnum.GOOGLE);
		user.setProviderId(providerId);
		return user;
	}

}
