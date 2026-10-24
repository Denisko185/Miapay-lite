package com.miapay.miapaylite.auth;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miapay.miapaylite.operator.OperatorAccountService;

@Service
public class ProfileService {

	private final UserRepository users;
	private final OperatorAccountService operatorAccounts;

	public ProfileService(UserRepository users, OperatorAccountService operatorAccounts) {
		this.users = users;
		this.operatorAccounts = operatorAccounts;
	}

	@Transactional(readOnly = true)
	public MeResponse profileOf(UUID userId) {
		User user = users.findById(userId)
				.orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userId));
		long balance = operatorAccounts.balanceOf(userId);
		return new MeResponse(user.getId(), user.getEmail(), user.getRole(), balance);
	}

}
