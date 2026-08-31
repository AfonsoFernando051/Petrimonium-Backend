package com.jf.PetApp.application.auth.exception;

public class AuthenticationException extends RuntimeException {

	private static final long serialVersionUID = 8795728549405982312L;
	
	public AuthenticationException( ) {
		super("Invalid email or password");
	}

	public AuthenticationException(String message) {
		super(message);
	}

}
