package com.miapay.miapaylite.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.miapay.miapaylite.TestcontainersConfiguration;

// End-to-end: only real HTTP calls, no token forged by the test. It proves that the token
// returned by POST /auth/login is the one GET /me accepts.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

	private static final String PASSWORD = "password123";

	@Autowired
	private MockMvc mockMvc;

	@ParameterizedTest
	@CsvSource({ "BUYER,100000", "MERCHANT,0" })
	void registerThenLoginThenMeWorksEndToEnd(Role role, long expectedBalance) throws Exception {
		String email = "flow-" + UUID.randomUUID() + "@example.com";

		// 1. Register: 201 and the response never contains the password.
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"" + role + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value(role.name()))
				.andExpect(jsonPath("$.password").doesNotExist());

		// 2. Login: keep the token exactly as the API returns it.
		String loginResponse = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(loginResponse, "$.accessToken");

		// 3. Call a protected route with that token: it must be accepted and identify the same user.
		mockMvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value(role.name()))
				.andExpect(jsonPath("$.balance").value(expectedBalance));
	}

}
