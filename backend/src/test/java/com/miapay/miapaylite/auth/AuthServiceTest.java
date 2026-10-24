package com.miapay.miapaylite.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.miapay.miapaylite.TestcontainersConfiguration;
import com.miapay.miapaylite.operator.OperatorAccount;
import com.miapay.miapaylite.operator.OperatorAccountRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository users;

	@Autowired
	private OperatorAccountRepository accounts;

	@Autowired
	private JwtDecoder jwtDecoder;

	private static final String PASSWORD = "password123";

	@Test
	void buyerStartsWithFundedOperatorAccount() {
		RegisterResponse response = authService.register(request(uniqueEmail(), Role.BUYER));

		OperatorAccount account = accounts.findByUserId(response.id()).orElseThrow();
		assertThat(account.getBalance()).isEqualTo(100_000L);
	}

	@Test
	void merchantStartsWithEmptyOperatorAccount() {
		RegisterResponse response = authService.register(request(uniqueEmail(), Role.MERCHANT));

		OperatorAccount account = accounts.findByUserId(response.id()).orElseThrow();
		assertThat(account.getBalance()).isZero();
	}

	@Test
	void emailIsNormalizedBeforeBeingStored() {
		String unique = UUID.randomUUID().toString();

		RegisterResponse response = authService.register(request("  Alice-" + unique + "@Example.COM ", Role.BUYER));

		assertThat(response.email()).isEqualTo("alice-" + unique + "@example.com");
	}

	@Test
	void duplicateEmailIsRefusedEvenWithDifferentCase() {
		String unique = UUID.randomUUID().toString();
		authService.register(request("bob-" + unique + "@example.com", Role.BUYER));

		assertThatThrownBy(() -> authService.register(request("BOB-" + unique + "@Example.com", Role.MERCHANT)))
				.isInstanceOf(EmailAlreadyUsedException.class);
	}

	@Test
	void loginWithCorrectCredentialsReturnsAValidToken() {
		String email = uniqueEmail();
		RegisterResponse registered = authService.register(request(email, Role.BUYER));

		LoginResponse response = authService.login(new LoginRequest(email, PASSWORD));

		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isPositive();
		assertThat(jwtDecoder.decode(response.accessToken()).getSubject()).isEqualTo(registered.id().toString());
	}

	// The most important login test: the happy path alone would also pass with a broken password check.
	@Test
	void loginWithWrongPasswordIsRefused() {
		String email = uniqueEmail();
		authService.register(request(email, Role.BUYER));

		assertThatThrownBy(() -> authService.login(new LoginRequest(email, "not-the-password")))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void loginWithUnknownEmailIsRefused() {
		assertThatThrownBy(() -> authService.login(new LoginRequest(uniqueEmail(), PASSWORD)))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void unknownEmailAndWrongPasswordGiveTheSameError() {
		String email = uniqueEmail();
		authService.register(request(email, Role.BUYER));

		Throwable wrongPassword = catchThrowable(() -> authService.login(new LoginRequest(email, "not-the-password")));
		Throwable unknownEmail = catchThrowable(() -> authService.login(new LoginRequest(uniqueEmail(), PASSWORD)));

		assertThat(wrongPassword).isInstanceOf(InvalidCredentialsException.class);
		assertThat(unknownEmail).isInstanceOf(wrongPassword.getClass()).hasMessage(wrongPassword.getMessage());
	}

	@Test
	void loginNormalizesTheEmail() {
		String unique = UUID.randomUUID().toString();
		authService.register(request("carol-" + unique + "@example.com", Role.BUYER));

		LoginResponse response = authService.login(new LoginRequest("  CAROL-" + unique + "@Example.COM ", PASSWORD));

		assertThat(response.accessToken()).isNotBlank();
	}

	// Proves that the UNIQUE constraint, not the existsByEmail pre-check, is what protects us:
	// several requests read "email not used" at the same time and all try to insert.
	// Repeated 10 times because a race condition may not show up on a single run.
	@RepeatedTest(10)
	void concurrentRegistrationsWithTheSameEmailCreateOneUser() throws Exception {
		int attempts = 10;
		// Same email for every thread, unique per repetition so runs do not interfere.
		String email = uniqueEmail();
		// One thread per attempt, so that all of them can really run at the same time.
		ExecutorService executor = Executors.newFixedThreadPool(attempts);
		// "ready" counts the threads that are waiting on the starting line.
		CountDownLatch ready = new CountDownLatch(attempts);
		// "start" is the starting gun: every thread blocks on it until it is released once.
		CountDownLatch start = new CountDownLatch(1);

		List<Future<RegisterResponse>> results = new ArrayList<>();
		for (int i = 0; i < attempts; i++) {
			results.add(executor.submit(() -> {
				ready.countDown();   // I am on the starting line
				start.await();       // wait for the gun, so that all threads leave together
				return authService.register(request(email, Role.BUYER));
			}));
		}
		// Wait until every thread is blocked on the starting line, then fire the gun.
		ready.await();
		start.countDown();

		int created = 0;
		int refused = 0;
		for (Future<RegisterResponse> result : results) {
			try {
				// get() rethrows, wrapped in an ExecutionException, what the thread threw.
				result.get();
				created++;
			}
			catch (ExecutionException ex) {
				// Only the expected business conflict is acceptable: any other failure
				// (database error, NullPointerException...) makes the test fail here.
				assertThat(ex.getCause()).isInstanceOf(EmailAlreadyUsedException.class);
				refused++;
			}
		}
		executor.shutdown();

		// Exactly one winner, and every other attempt got a clean "email already used".
		assertThat(created).isEqualTo(1);
		assertThat(refused).isEqualTo(attempts - 1);

		// The winner's user and operator account were created together in one transaction.
		User user = users.findByEmail(email).orElseThrow();
		assertThat(accounts.findByUserId(user.getId())).isPresent();
	}

	private static RegisterRequest request(String email, Role role) {
		return new RegisterRequest(email, PASSWORD, role);
	}

	private static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}

}
