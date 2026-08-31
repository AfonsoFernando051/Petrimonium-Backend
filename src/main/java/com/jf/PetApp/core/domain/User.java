package com.jf.PetApp.core.domain;

import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import com.jf.PetApp.core.domain.enums.AuthProviderEnum;
import com.jf.PetApp.core.domain.enums.RoleEnum;

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
	Pet pet;

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
	 * How this user authenticates. LOCAL users have a password; GOOGLE users
	 * authenticate via a verified Google ID token and have none.
	 */
	AuthProviderEnum provider = AuthProviderEnum.LOCAL;

	/**
	 * Google's `sub` claim, uniquely identifying the Google account. Null for LOCAL users.
	 */
	String providerId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Pet getPet() {
		return pet;
	}

	public void setPet(Pet pet) {
		this.pet = pet;
	}

	public Finance getFinance() {
		return finance;
	}

	public void setFinance(Finance finance) {
		this.finance = finance;
	}

	public RoleEnum getRole() {
		return role;
	}

	public void setRole(RoleEnum role) {
		this.role = role;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public boolean hasAnsweredOnboarding() {
		return hasAnsweredOnboarding;
	}

	public void setHasAnsweredOnboarding(boolean hasAnsweredOnboarding) {
		this.hasAnsweredOnboarding = hasAnsweredOnboarding;
	}

	public InvestorProfile getInvestorProfile() {
		return investorProfile;
	}

	public void setInvestorProfile(InvestorProfile investorProfile) {
		this.investorProfile = investorProfile;
	}

	public String getPreferredLanguage() {
		return preferredLanguage;
	}

	public void setPreferredLanguage(String preferredLanguage) {
		this.preferredLanguage = preferredLanguage;
	}

	public AuthProviderEnum getProvider() {
		return provider;
	}

	public void setProvider(AuthProviderEnum provider) {
		this.provider = provider;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
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
