package com.jf.PetApp.application.auth.exception;

public class UserAlreadyExistsException extends RuntimeException {

	private static final long serialVersionUID = 8795728549405982312L;
	
	public UserAlreadyExistsException( ) {
		super("User already exists");
	}

}
