package com.veris.inventory.platform.config;

import java.net.URI;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Connection metadata only; no authentication flow is activated in Phase 01. */
@Validated
@ConfigurationProperties(prefix = "app.identity")
public record IdentityProperties(@NotNull URI issuerUri, @NotNull URI discoveryUri) {
}
