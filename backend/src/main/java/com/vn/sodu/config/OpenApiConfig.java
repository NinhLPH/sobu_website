package com.vn.sodu.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Sobu Store API",
                version = "1.0",
                description = "REST API for Sobu Store — product catalog, cart, orders, payments, shipping, and Nhanh POS integration.",
                contact = @Contact(
                        name = "Sobu Support",
                        email = "support@sobu.vn"
                ),
                license = @License(
                        name = "Proprietary"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8081", description = "Local development"),
                @Server(url = "https://api.sobu.vn", description = "Production")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
