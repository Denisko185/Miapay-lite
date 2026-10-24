package com.miapay.miapaylite.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.miapay.miapaylite.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RegisterApiTest {

	@Autowired
	private MockMvc mockMvc;

	static Stream<Arguments> invalidBodies() {
		String ok = "user@example.com";
		return Stream.of(
				Arguments.of("malformed email", body("not-an-email", "password123", "\"BUYER\"")),
				Arguments.of("blank email", body("", "password123", "\"BUYER\"")),
				Arguments.of("password too short", body(ok, "short12", "\"BUYER\"")),
				Arguments.of("password longer than 72 characters", body(ok, "a".repeat(73), "\"BUYER\"")),
				Arguments.of("missing role", body(ok, "password123", "null")),
				Arguments.of("unknown role", body(ok, "password123", "\"ADMIN\"")),
				Arguments.of("malformed JSON", "{\"email\": "));
	}

	@ParameterizedTest(name = "{0} -> 400")
	@MethodSource("invalidBodies")
	void invalidRequestsAreRejectedWithAProblemDetail(String description, String body) throws Exception {
		register(body)
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void duplicateEmailIsRefusedWithAConflictEvenWithADifferentCase() throws Exception {
		String unique = UUID.randomUUID().toString();
		register(body("dave-" + unique + "@example.com", "password123", "\"BUYER\"")).andExpect(status().isCreated());

		register(body("DAVE-" + unique + "@Example.com", "password123", "\"MERCHANT\""))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409));
	}

	private ResultActions register(String body) throws Exception {
		return mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	// role is passed as raw JSON ("\"BUYER\"" or "null") so that invalid values can be sent too.
	private static String body(String email, String password, String roleJson) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"role\":" + roleJson + "}";
	}

}
