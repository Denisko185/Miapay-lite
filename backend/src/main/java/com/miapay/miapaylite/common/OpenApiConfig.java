package com.miapay.miapaylite.common;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

// Declares the Bearer JWT scheme so that Swagger UI shows an "Authorize" button and sends
// "Authorization: Bearer <token>" on the calls made from the page.
// The server URL is relative ("/"): Swagger UI calls the same origin as the page itself. Behind a proxy
// such as Codespaces, the generated absolute URL would be http://localhost:8080, which the browser cannot reach.
@Configuration
@OpenAPIDefinition(
		security = @SecurityRequirement(name = "bearerAuth"),
		servers = @Server(url = "/"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

}
