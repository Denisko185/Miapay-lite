package com.miapay.miapaylite.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Email @Size(max = 255) String email,
		// BCrypt only uses the first 72 bytes of a password, so we refuse longer ones.
		@NotBlank @Size(min = 8, max = 72) String password,
		@NotNull Role role) {
}
