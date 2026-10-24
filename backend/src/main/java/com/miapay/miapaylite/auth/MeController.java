package com.miapay.miapaylite.auth;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

	private final ProfileService profiles;

	public MeController(ProfileService profiles) {
		this.profiles = profiles;
	}

	// The identity comes from the validated token ("sub" claim), never from a request parameter.
	@GetMapping("/me")
	public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
		return profiles.profileOf(UUID.fromString(jwt.getSubject()));
	}

}
