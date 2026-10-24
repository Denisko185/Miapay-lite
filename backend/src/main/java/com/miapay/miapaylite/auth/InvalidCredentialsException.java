package com.miapay.miapaylite.auth;

// Thrown for an unknown email AND for a wrong password: the caller must not be able to tell them apart.
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid email or password");
	}

}
