package com.veris.inventory.config;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades de {@code app.security.*}: URLs públicas e internas del flujo
 * OIDC/BFF, secreto del cliente público, organización configurada, timeout de
 * sesión absoluta y credenciales del cliente de servicio de la Admin API de
 * Keycloak. Nunca se registran ni se exponen sus valores.
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @NotNull String publicUrl,
        @NotNull String oidcInternalUri,
        @NotNull String clientSecret,
        @NotNull String organizationId,
        @NotNull Duration absoluteSessionTimeout,
        @NotNull String adminClientId,
        @NotNull String adminClientSecret) {
}
