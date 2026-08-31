package com.jf.PetApp.core.domain;

import com.jf.PetApp.core.domain.enums.PetSpecieEnum;

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
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public PetSpecieEnum getSpecie() {
		return specie;
	}
	public void setSpecie(PetSpecieEnum specie) {
		this.specie = specie;
	}
	public int getHealth() {
		return health;
	}
	public void setHealth(int health) {
		this.health = health;
	}
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	
}
