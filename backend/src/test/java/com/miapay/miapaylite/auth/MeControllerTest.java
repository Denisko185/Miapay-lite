package com.miapay.miapaylite.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.miapay.miapaylite.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository users;

	@Autowired
	private JwtService jwtService;

	@Test
	void meRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void buyerSeesTheProfileAndTheStartingBalance() throws Exception {
		User buyer = registeredUser(Role.BUYER);

		mockMvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, bearer(buyer)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(buyer.getId().toString()))
				.andExpect(jsonPath("$.email").value(buyer.getEmail()))
				.andExpect(jsonPath("$.role").value("BUYER"))
				.andExpect(jsonPath("$.balance").value(100_000));
	}

	@Test
	void merchantStartsWithAnEmptyBalance() throws Exception {
		User merchant = registeredUser(Role.MERCHANT);

		mockMvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, bearer(merchant)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("MERCHANT"))
				.andExpect(jsonPath("$.balance").value(0));
	}

	@Test
	void theResponseNeverExposesThePasswordOrItsHash() throws Exception {
		User buyer = registeredUser(Role.BUYER);

		mockMvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, bearer(buyer)))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	// The identity must come from the token only: a userId in the request must change nothing.
	@Test
	void aUserIdParameterCannotImpersonateAnotherUser() throws Exception {
		User caller = registeredUser(Role.BUYER);
		User victim = registeredUser(Role.MERCHANT);

		mockMvc.perform(get("/me")
				.param("userId", victim.getId().toString())
				.header(HttpHeaders.AUTHORIZATION, bearer(caller)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(caller.getId().toString()))
				.andExpect(jsonPath("$.email").value(caller.getEmail()));
	}

	private String bearer(User user) {
		return "Bearer " + jwtService.generateToken(user);
	}

	private User registeredUser(Role role) {
		String email = "me-" + UUID.randomUUID() + "@example.com";
		authService.register(new RegisterRequest(email, "password123", role));
		return users.findByEmail(email).orElseThrow();
	}

}
