package com.miapay.miapaylite.auth;

public enum Role {

	BUYER(100_000),
	MERCHANT(0);

	// Starting balance of the operator account, in integer FCFA.
	private final long initialBalance;

	Role(long initialBalance) {
		this.initialBalance = initialBalance;
	}

	public long getInitialBalance() {
		return initialBalance;
	}

}
