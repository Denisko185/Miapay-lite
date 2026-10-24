package com.miapay.miapaylite.auth;

public class EmailAlreadyUsedException extends RuntimeException {

	public EmailAlreadyUsedException(String email) {
		super("Email already used: " + email);
	}

}
