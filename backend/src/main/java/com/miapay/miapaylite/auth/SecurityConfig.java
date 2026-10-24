package com.miapay.miapaylite.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	// Turns the "role" claim of the JWT (e.g. "MERCHANT") into the authority "ROLE_MERCHANT",
	// which is what hasRole("MERCHANT") checks against.
	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName("role");
		authorities.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	// Role-based rules (/qr, /payments...) will be added here later, together with their endpoints.
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
			throws Exception {
		http
				// CSRF attacks rely on the browser sending a session cookie by itself. We use a Bearer token
				// that the client adds explicitly, so there is nothing for an attacker to ride on.
				.csrf(csrf -> csrf.disable())
				// No HTTP session: every request must carry its own token.
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// Rules are checked in order and the first match wins, so anyRequest() must come last.
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/auth/**").permitAll()
						.requestMatchers("/actuator/health", "/actuator/info").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.anyRequest().authenticated())
				// Validate incoming "Authorization: Bearer <jwt>" headers with the JwtDecoder bean.
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
		return http.build();
	}

}
