package com.veris.inventory.health;

import java.time.Duration;
import java.util.Map;

import com.veris.inventory.config.IdentityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Verifies OIDC discovery reachability, without obtaining or validating user tokens. */
@Component("keycloak")
@EnableConfigurationProperties(IdentityProperties.class)
public class KeycloakHealthIndicator implements HealthIndicator {
    private final IdentityProperties properties;
    private final RestClient client;

    public KeycloakHealthIndicator(IdentityProperties properties) {
        this.properties = properties;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public Health health() {
        try {
            var issuer = properties.issuerUri().toString();
            var metadata = client.get().uri(properties.discoveryUri())
                    .retrieve().body(Map.class);
            if (metadata != null && issuer.equals(metadata.get("issuer"))
                    && metadata.get("jwks_uri") instanceof String) {
                return Health.up().build();
            }
            return Health.down().build();
        } catch (RuntimeException exception) {
            // Do not expose URLs, internal exceptions or connection details in health output.
            return Health.down().build();
        }
    }
}
