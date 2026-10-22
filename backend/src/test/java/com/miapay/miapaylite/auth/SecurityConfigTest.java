package com.miapay.miapaylite.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.web.servlet.MockMvc;

import com.miapay.miapaylite.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository users;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private JwtAuthenticationConverter jwtAuthenticationConverter;

	// No CSRF token is sent: this fails with 403 if CSRF protection is still active.
	@Test
	void registerIsPublicAndNeedsNoCsrfToken() throws Exception {
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(uniqueEmail(), "BUYER")))
				.andExpect(status().isCreated());
	}

	@Test
	void loginIsPublicReturnsATokenAndCreatesNoSession() throws Exception {
		String email = uniqueEmail();
		authService.register(new RegisterRequest(email, "password123", Role.BUYER));

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(email, null)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				// Stateless: no JSESSIONID cookie must be set.
				.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
	}

	@Test
	void wrongPasswordOnLoginIsUnauthorized() throws Exception {
		String email = uniqueEmail();
		authService.register(new RegisterRequest(email, "password123", Role.BUYER));

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"not-the-password\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void healthIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void apiDocsArePublic() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	// "/anything" has no controller: 401 proves security answers before routing.
	@Test
	void otherRoutesRequireAuthentication() throws Exception {
		mockMvc.perform(get("/anything")).andExpect(status().isUnauthorized());
	}

	// 404 (and not 401/403) proves the token was accepted and the request reached the routing.
	@Test
	void aValidTokenIsAccepted() throws Exception {
		String token = jwtService.generateToken(registeredUser(Role.BUYER));

		mockMvc.perform(get("/anything").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void aTamperedTokenIsUnauthorized() throws Exception {
		String token = jwtService.generateToken(registeredUser(Role.BUYER));
		char last = token.charAt(token.length() - 1);
		String tampered = token.substring(0, token.length() - 1) + (last == 'A' ? 'B' : 'A');

		mockMvc.perform(get("/anything").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void theRoleClaimBecomesARoleAuthority() {
		String token = jwtService.generateToken(registeredUser(Role.MERCHANT));

		var authentication = jwtAuthenticationConverter.convert(jwtDecoder.decode(token));

		// Spring Security 7 also adds a FACTOR_BEARER authority, so we check for ours without requiring an exact list.
		assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.contains("ROLE_MERCHANT")
				.doesNotContain("ROLE_BUYER");
	}

	private User registeredUser(Role role) {
		String email = uniqueEmail();
		authService.register(new RegisterRequest(email, "password123", role));
		return users.findByEmail(email).orElseThrow();
	}

	private static String uniqueEmail() {
		return "sec-" + UUID.randomUUID() + "@example.com";
	}

	// Builds a JSON body; a null role means a login request (no role field).
	private static String json(String email, String role) {
		String roleField = (role == null) ? "" : ",\"role\":\"" + role + "\"";
		return "{\"email\":\"" + email + "\",\"password\":\"password123\"" + roleField + "}";
	}

}
