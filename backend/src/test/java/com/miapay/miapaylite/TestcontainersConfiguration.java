package com.miapay.miapaylite;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
	}

	// Tests do not read .env, so they get a fixed secret (>= 32 bytes) here instead.
	@Bean
	DynamicPropertyRegistrar jwtTestSecret() {
		return registry -> registry.add("app.jwt.secret", () -> "test-only-secret-that-is-at-least-32-bytes-long");
	}

}
