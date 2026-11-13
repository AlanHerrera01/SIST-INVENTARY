package com.veris.inventory.access.services.servicesImpl;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.veris.inventory.access.services.IdentityAdminGateway;
import com.veris.inventory.common.error.ApiException;
import com.veris.inventory.config.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Adaptador de la Admin REST API de Keycloak con el cliente de servicio
 * {@code inventory-admin} (client_credentials). El realm se deriva del issuer
 * público; las llamadas usan la URI interna del despliegue.
 *
 * <p>El subject que se almacena localmente es el {@code id} del usuario en
 * Keycloak, que el proveedor emite como claim {@code sub} del ID token.</p>
 */
@Component
public class KeycloakAdminGateway implements IdentityAdminGateway {
    private static final Logger LOG = LoggerFactory.getLogger(KeycloakAdminGateway.class);

    private final RestClient http;
    private final String tokenUri;
    private final String adminBase;
    private final String clientId;
    private final String clientSecret;
    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    public KeycloakAdminGateway(SecurityProperties security) {
        this.clientId = security.adminClientId();
        this.clientSecret = security.adminClientSecret();
        var internal = URI.create(security.oidcInternalUri());
        var realm = internal.getPath().replaceFirst(".*/realms/", "");
        var authority = internal.getScheme() + "://" + internal.getAuthority();
        this.adminBase = authority + "/admin/realms/" + realm;
        this.tokenUri = internal.toString().replaceAll("/$", "")
                + "/protocol/openid-connect/token";
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(10000);
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public Optional<ExternalUser> findBySubject(String subject) {
        try {
            var user = get("users/" + subject).retrieve().body(UserRepresentation.class);
            return Optional.ofNullable(user).map(KeycloakAdminGateway::toExternal);
        } catch (RestClientResponseException notFound) {
            if (notFound.getStatusCode().value() == 404) return Optional.empty();
            throw unavailable("No se pudo consultar el usuario en el proveedor.");
        } catch (RestClientException failure) {
            throw unavailable("Proveedor de identidad no disponible.");
        }
    }

    @Override
    public Optional<ExternalUser> findByUsername(String username) {
        try {
            var users = get("users?username="
                            + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8)
                            + "&exact=true")
                    .retrieve().body(new ParameterizedTypeReference<List<UserRepresentation>>() {});
            return users == null || users.isEmpty() ? Optional.empty()
                    : Optional.of(toExternal(users.getFirst()));
        } catch (RestClientException failure) {
            throw unavailable("Proveedor de identidad no disponible.");
        }
    }

    @Override
    public ExternalUser create(NewUser user) {
        var body = new LinkedHashMap<String, Object>();
        body.put("username", user.username());
        if (user.email() != null) body.put("email", user.email());
        body.put("firstName", user.firstName());
        body.put("lastName", user.lastName());
        body.put("enabled", user.enabled());
        body.put("emailVerified", false);
        try {
            var response = post("users").contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().toEntity(Void.class);
            var location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
            var subject = location == null ? null : location.substring(location.lastIndexOf('/') + 1);
            if (subject == null || subject.isBlank()) {
                throw unavailable("El proveedor no devolvió el identificador del usuario.");
            }
            if (user.temporaryPassword() != null) {
                resetPassword(subject, user.temporaryPassword());
            }
            return findBySubject(subject).orElseThrow(() ->
                    unavailable("No se pudo leer el usuario creado en el proveedor."));
        } catch (RestClientResponseException conflict) {
            if (conflict.getStatusCode().value() == 409) {
                throw ApiException.conflict("IDENTITY_USERNAME_TAKEN",
                        "El nombre de usuario ya existe en el proveedor de identidad.");
            }
            throw unavailable("El proveedor rechazó la creación del usuario.");
        } catch (RestClientException failure) {
            throw unavailable("Proveedor de identidad no disponible.");
        }
    }

    @Override
    public void updateProfile(String subject, String firstName, String email) {
        updateUser(subject, Map.of("firstName", firstName, "email", email));
    }

    @Override
    public void setEnabled(String subject, boolean enabled) {
        updateUser(subject, Map.of("enabled", enabled));
    }

    @Override
    public void logout(String subject) {
        try {
            post("users/" + subject + "/logout").retrieve().toBodilessEntity();
        } catch (RestClientException failure) {
            throw unavailable("No se pudieron cerrar las sesiones del usuario.");
        }
    }

    @Override
    public void resetPassword(String subject, String temporaryPassword) {
        try {
            post("users/" + subject + "/reset-password").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("type", "password", "value", temporaryPassword, "temporary", true))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException failure) {
            throw unavailable("No se pudo fijar la contraseña temporal.");
        }
    }

    @Override
    public void delete(String subject) {
        try {
            http.delete().uri(adminBase + "/users/" + subject)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                    .retrieve().toBodilessEntity();
        } catch (RestClientException failure) {
            throw unavailable("No se pudo eliminar el usuario del proveedor.");
        }
    }

    // ---- internos ----

    /** GET autenticado contra la Admin API. */
    private RestClient.RequestHeadersSpec<?> get(String path) {
        return http.get().uri(adminBase + "/" + path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token());
    }

    /** POST autenticado contra la Admin API (sin cuerpo por defecto). */
    private RestClient.RequestBodySpec post(String path) {
        return http.post().uri(adminBase + "/" + path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token());
    }

    private void updateUser(String subject, Map<String, Object> changes) {
        try {
            var current = get("users/" + subject).retrieve().body(UserRepresentation.class);
            if (current == null) {
                throw unavailable("El usuario no existe en el proveedor.");
            }
            var payload = new LinkedHashMap<String, Object>();
            payload.put("username", current.username());
            payload.put("email", changes.containsKey("email") ? changes.get("email") : current.email());
            payload.put("firstName", changes.containsKey("firstName") ? changes.get("firstName") : current.firstName());
            payload.put("lastName", current.lastName());
            payload.put("enabled", changes.containsKey("enabled") ? changes.get("enabled") : current.enabled());
            payload.put("emailVerified", false);
            http.put().uri(adminBase + "/users/" + subject)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON).body(payload)
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException missing) {
            if (missing.getStatusCode().value() == 404) {
                throw unavailable("El usuario no existe en el proveedor.");
            }
            throw unavailable("El proveedor rechazó la actualización.");
        } catch (RestClientException failure) {
            throw unavailable("Proveedor de identidad no disponible.");
        }
    }

    private String token() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(30))) {
            return cachedToken;
        }
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        try {
            var response = http.post().uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                    .retrieve().body(TokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw unavailable("El proveedor no emitió token de servicio.");
            }
            cachedToken = response.accessToken();
            tokenExpiry = Instant.now().plusSeconds(Math.max(response.expiresIn() - 30, 30));
            return cachedToken;
        } catch (RestClientException failure) {
            LOG.warn("No se pudo obtener token de servicio de Keycloak: {}", failure.toString());
            throw unavailable("Proveedor de identidad no disponible.");
        }
    }

    private static ApiException unavailable(String detail) {
        return ApiException.unavailable("IDENTITY_PROVIDER_UNAVAILABLE", detail);
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(@com.fasterxml.jackson.annotation.JsonProperty("access_token") String accessToken,
                                 @com.fasterxml.jackson.annotation.JsonProperty("expires_in") int expiresIn) {}

    /** Representación mínima del usuario de Keycloak (solo campos usados). */
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private record UserRepresentation(String id, String username, String email, Boolean enabled,
                                      String firstName, String lastName) {}

    private static ExternalUser toExternal(UserRepresentation user) {
        return new ExternalUser(user.id(), user.username(), user.email(),
                Boolean.TRUE.equals(user.enabled()), user.firstName(), user.lastName());
    }
}
