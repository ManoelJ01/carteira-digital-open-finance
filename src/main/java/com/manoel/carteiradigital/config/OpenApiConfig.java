package com.manoel.carteiradigital.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Carteira Digital — Agregador Multibanco (Open Finance)",
                version = "v1",
                description = "API RESTful que consolida saldos, extratos e categorização financeira " +
                        "de múltiplas contas bancárias de um usuário via integração com a Pluggy (Open Finance).",
                contact = @Contact(name = "Manoel Juvino dos Santos Neto")
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
