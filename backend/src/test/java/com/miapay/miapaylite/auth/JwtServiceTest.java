package com.miapay.miapaylite.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.miapay.miapaylite.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JwtServiceTest {

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository users;

	@Test
	void generatedTokenIsAcceptedByTheDecoderAndCarriesTheUserClaims() {
		User user = registeredUser(Role.MERCHANT);

		Jwt jwt = jwtDecoder.decode(jwtService.generateToken(user));

		assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
		assertThat(jwt.<String>getClaim("role")).isEqualTo("MERCHANT");
		assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toSeconds())
				.isEqualTo(jwtService.expiresInSeconds());
	}

	@Test
	void tamperedTokenIsRejected() {
		String token = jwtService.generateToken(registeredUser(Role.BUYER));
		// Change the last character of the signature.
		char last = token.charAt(token.length() - 1);
		String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

		assertThatThrownBy(() -> jwtDecoder.decode(tampered)).isInstanceOf(JwtException.class);
	}

	private User registeredUser(Role role) {
		String email = "jwt-" + UUID.randomUUID() + "@example.com";
		authService.register(new RegisterRequest(email, "password123", role));
		return users.findByEmail(email).orElseThrow();
	}

}
