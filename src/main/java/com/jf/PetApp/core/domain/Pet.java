package com.jf.PetApp.core.domain;

import com.jf.PetApp.core.domain.enums.PetSpecieEnum;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Pet {

	/**
	 * Id for pet
	 */
	Integer id;

	/**
	 * Name for pet
	 */
	String name;

	/**
	 * Specie for pet
	 */
	PetSpecieEnum specie;

	/**
	 * Pet health
	 */
	int health;

	/**
	 * The user's pet
	 */
	User user;
}
