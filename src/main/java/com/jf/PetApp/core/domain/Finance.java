package com.jf.PetApp.core.domain;

import java.math.BigDecimal;
import java.util.Collection;

public class Finance {

	/**
	 * Id for finance
	 */
	int id;

	/**
	 * Balance for finance
	 */
	BigDecimal balance;

	/**
	 * Collection of investments
	 */
	Collection<Investment> investments;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public Collection<Investment> getInvestments() {
		return investments;
	}

	public void setInvestments(Collection<Investment> investments) {
		this.investments = investments;
	}

}
