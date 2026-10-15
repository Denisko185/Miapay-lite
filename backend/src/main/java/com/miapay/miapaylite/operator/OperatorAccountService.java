package com.miapay.miapaylite.operator;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperatorAccountService {

	private final OperatorAccountRepository accounts;

	public OperatorAccountService(OperatorAccountRepository accounts) {
		this.accounts = accounts;
	}

	// MANDATORY: joins the caller's transaction, so the account is rolled back with the user.
	@Transactional(propagation = Propagation.MANDATORY)
	public OperatorAccount openAccount(UUID userId, long initialBalance) {
		return accounts.save(new OperatorAccount(userId, initialBalance));
	}

}
