package com.miapay.miapaylite.auth;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miapay.miapaylite.operator.OperatorAccountService;

@Service
public class AuthService {

	private final UserRepository users;
	private final OperatorAccountService operatorAccounts;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final String fakeHashedPwd;

	public AuthService(UserRepository users, OperatorAccountService operatorAccounts, PasswordEncoder passwordEncoder,
			JwtService jwtService) {
		this.users = users;
		this.operatorAccounts = operatorAccounts;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.fakeHashedPwd = passwordEncoder.encode("fake@password");
	}

	@Transactional
	public RegisterResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());

		// Fast path for the common case. The UNIQUE constraint remains the real guard.
		if (users.existsByEmail(email)) {
			throw new EmailAlreadyUsedException(email);
		}

		String passwordHash = passwordEncoder.encode(request.password());

		User user;
		try {
			// saveAndFlush: the INSERT must happen here, otherwise the constraint violation
			// would only surface at commit time, outside this try/catch.
			user = users.saveAndFlush(new User(email, passwordHash, request.role()));
		}
		catch (DataIntegrityViolationException ex) {
			// A concurrent registration with the same email won the race.
			throw new EmailAlreadyUsedException(email);
		}

		operatorAccounts.openAccount(user.getId(), user.getRole().getInitialBalance());

		return new RegisterResponse(user.getId(), user.getEmail(), user.getRole());
	}

	public LoginResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());

		User user = users.findByEmail(email).orElse(null);
		String hashedPwd = (user != null) ? user.getPasswordHash() : fakeHashedPwd;
		boolean isMatch = passwordEncoder.matches(request.password(), hashedPwd);
		if (user == null || !isMatch) {
			throw new InvalidCredentialsException();
		}
		return new LoginResponse(jwtService.generateToken(user), "Bearer", jwtService.expiresInSeconds());
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
