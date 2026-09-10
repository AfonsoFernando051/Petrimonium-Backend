package com.jf.PetApp.core.domain;

import java.math.BigDecimal;
import java.util.Collection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

}
