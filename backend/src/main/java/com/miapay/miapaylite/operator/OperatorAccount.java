package com.miapay.miapaylite.operator;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "operator_accounts")
public class OperatorAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	// Only the id of the owner: the operator module does not depend on the auth module.
	@Column(name = "user_id", nullable = false, unique = true)
	private UUID userId;

	// Integer FCFA, never a floating-point number.
	@Column(nullable = false)
	private long balance;

	@Version
	private long version;

	protected OperatorAccount() {
		// required by JPA
	}

	public OperatorAccount(UUID userId, long balance) {
		this.userId = userId;
		this.balance = balance;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public long getBalance() {
		return balance;
	}

	public long getVersion() {
		return version;
	}

}
